#!/usr/bin/env python

"""
Requires Python 3.6

Usage: $0 [options] PATIENT_ID VCF CREDENTIALS_FILE SERVER_NAME

Upload the VCF file as an attachment for the given patient. For example:

    $0 [options] P0000102 path/to/file.vcf path/to/credentials.txt phenomecentral.org
    $0 [options] P0000102 path/to/file.vcf path/to/credentials.txt localhost:8080

-f, --force-reupload    Option for re-upload the file, even if it has been already uploaded
-p, --path              Option for specifying the context path
                        (* if leading slash is followed by a letter, additional slash has do be added, 
                          ex. for context path "/app" has to be -p '//app' )

credentials.txt should have one line with the form:

    username:password
"""

from __future__ import with_statement

import sys
import os
import logging
import json
import ssl

from http.client import HTTPConnection
from http.client import HTTPSConnection
from urllib.parse import urlencode
from base64 import b64encode
import xml.etree.ElementTree as ET

# Set globally after parsing arguments
CREDENTIALS = None
SERVER = None
PATH = None

def load_credentials(filename):
    global CREDENTIALS
    logging.info('Using credentials in file: {0}'.format(filename))
    with open(filename) as ifp:
        CREDENTIALS = ifp.read().strip()

class Request(object):
    def __init__(self, method, resource, content_type='text/plain', **kwargs):
        auth = b64encode(CREDENTIALS.encode()).decode()
        self._headers = {
            'Authorization': 'Basic {0}'.format(auth),
            'Content-Type': content_type,
            'Accept': '*/*'
            }
        self._method = method
        self._href = resource
        self._kwargs = kwargs
        self._conn = None

    def __enter__(self):
        # First try encrypted HTTPS protocol
        self._conn = HTTPSConnection(SERVER)
        #self._conn.set_debuglevel(1)
        try:
            self._conn.request(self._method, self._href, headers=self._headers, **self._kwargs)
        except ssl.SSLError as err:
            # Catching possible ssl.SSLError: [SSL: UNKNOWN_PROTOCOL] unknown protocol
            # in case the port is listening for unencrypted HTTP protocol
            self._conn = HTTPConnection(SERVER)
            self._conn.request(self._method, self._href, headers=self._headers, **self._kwargs)
        response = self._conn.getresponse()
        return response

    def __exit__(self, *args, **kwargs):
        if self._conn:
            self._conn.close()

def get_consent(record_id):
    logging.info('Loading consents for record: {0}'.format(record_id))
    href = PATH + '/rest/patients/{0}/consents/'.format(record_id)
    result = 'yes'
    with Request('GET', href) as response:

        assert response.status == 200, \
            'ERROR loading consents for record: {0}, {1}'.format(record_id, response.headers)
        
        logging.info('Consents for record: {0}'.format(record_id))
        # Convert bytes to string type and string type to dict
        string = response.read().decode('utf-8')
        json_obj = json.loads(string)
        for consent in json_obj:
            if consent['id'] == 'genetic':
                result = consent['status']
    return result

def get_vcf_object(record_id):
    href = PATH + '/rest/wikis/xwiki/spaces/data/pages/{0}/objects/PhenoTips.VCF'.format(record_id)
    result = {}
    with Request('GET', href) as response:
        if response.status == 500:
            # No VCF object for patient
            logging.info('No PhenoTips.VCF object for record: {0}'.format(record_id))
            return result

        assert response.status == 200, \
            'ERROR getting PhenoTips.VCF object for record: {0}, {1}'.format(record_id, response.headers)

        logging.info('Found PhenoTips.VCF object for record: {0}'.format(record_id))
        root = ET.fromstring(response.read())

    summary = root.find('{http://www.xwiki.org}objectSummary')

    if not summary:
        logging.info('No VCF object exist in record: {0}'.format(record_id))
        return None

    for link in summary.findall('{http://www.xwiki.org}link'):
        if link.get('rel') == 'http://www.xwiki.org/rel/object':
            href = link.get('href')
            logging.info('Found VCF object for {0}: {1}'.format(record_id, href))
            result['href'] = href
            break

    result['headline'] = summary.findtext('{http://www.xwiki.org}headline', '').strip()
    return result

def set_vcf_properties(record_id, vcf_object, filename, reference_genome='GRCh37'):
    method = 'PUT'
    href = vcf_object.get('href') if vcf_object else None
    if not href:
        # Create VCF missing properties object
        method = 'POST'
        href = PATH + '/rest/wikis/xwiki/spaces/data/pages/{0}/objects'.format(record_id)

    filebase = os.path.basename(filename)
    params = {
        'className': 'PhenoTips.VCF',
        'property#filename': filebase,
        'property#reference_genome': reference_genome,
        }

    content_type = 'application/x-www-form-urlencoded'

    with Request(method, href, content_type=content_type, body=urlencode(params)) as response:
        logging.info('RESPONSE: {0}'.format(href))
        assert response.status in [201, 202], \
            'Unexpected response ({0}) from setting VCF properties'.format(response.status)

        if response.status == 201:
            href = response.getheader('Location')
            logging.info('Created VCF object: {0}'.format(href))
            return href
        elif response.status == 202:
            logging.info('Updated VCF object: {0}'.format(href))
            return vcf_object

def delete_old_vcf_file(record_id):
    attachment_name = '{0}.vcf'.format(record_id)
    href = PATH + '/rest/wikis/xwiki/spaces/data/pages/{0}/attachments/{1}'.format(record_id, attachment_name)
    with Request('HEAD', href) as response:
        if response.status in [200, 500]:
            logging.info('Found linked old-style VCF: {0}'.format(attachment_name))
        else:
            return

    with Request('DELETE', href) as response:
        assert response.status in [204, 500], \
            'Unexpected response ({0}) from deleting VCF file'.format(response.status)
        logging.info('Deleted attachment: {0}'.format(attachment_name))

    with Request('DELETE', href) as response:
        assert response.status == 404, \
            'Failed to delete existing old-style VCF file'.format(response.status)

def upload_vcf_file(record_id, filename):
    attachment_name = os.path.basename(filename)
    href = PATH + '/rest/wikis/xwiki/spaces/data/pages/{0}/attachments/{1}'.format(record_id, attachment_name)
    with open(filename) as file:
        with Request('PUT', href, content_type='text/plain', body=file) as response:
            assert response.status in [201, 202], \
                'Unexpected response ({0}) from uploading VCF file'.format(response.status)
            if response.status == 201:
                logging.info('Created attachment: {0}'.format(attachment_name))
            elif response.status == 202:
                logging.info('Updated attachment: {0}'.format(attachment_name))

def script(record_id, vcf_filename, credentials_file, server_name, force_reupload, path):
    global SERVER
    SERVER = server_name
    global PATH
    PATH = path

    load_credentials(credentials_file)
    
    consent_granted = get_consent(record_id)
    if consent_granted == 'no':
        logging.info('Record {0} has not granted genetic consent, can not upload VCF'.format(record_id))
        return

    vcf_object = get_vcf_object(record_id)

    vcf_basename = os.path.basename(vcf_filename)
    if vcf_object and vcf_object.get('headline', '') == vcf_basename and force_reupload == False:
        # Filename already appears to be properly linked to record, skip
        logging.info('Record {0} already has linked VCF file: {1}'.format(record_id, vcf_basename))
    else:
        href = set_vcf_properties(record_id, vcf_object, vcf_filename)

        if href:
            upload_vcf_file(record_id, vcf_filename)

    # Optional, remove old VCF file
    # delete_old_vcf_file(record_id)


def parse_args(args):
    from optparse import OptionParser
    usage = "usage: %prog [options] PATIENT_ID VCF CREDENTIALS_FILE SERVER_NAME"
    parser = OptionParser(usage=usage)
    parser.add_option("-f", "--force-reupload", dest="force_reupload", default=False, action='store_true',
                      help="Option for re-upload the file, even if it has been already uploaded")
    parser.add_option("-p", "--path", dest="path", default='',
                      help="Option for specifying the context path")
    (options, args) = parser.parse_args()
    logging.info('Options: {0} ; args: {1}'.format(options, args))

    if len(args) != 4:
        parser.error('Invalid number of arguments')

    return (options, args)

def main(args=sys.argv[1:]):
    logging.basicConfig(level=logging.DEBUG)
    options, args = parse_args(args)
    kwargs = dict(options.__dict__)
    script(*args, **kwargs)

if __name__ == '__main__':
    sys.exit(main())
