/*
 * HPOTerm is a class for storing phenotype information and loading it from the
 * the HPO database. These phenotypes can be attributed to an individual in the Pedigree.
 *
 * @param hpoID the id number for the HPO term, taken from the HPO database
 * @param name a string representing the name of the term e.g. "Abnormality of the eye"
 */
define([
        "pedigree/model/helpers"
    ], function(
        Helpers
    ){
    var HPOTerm = Class.create( {

        initialize: function(hpoID, name, callWhenReady) {
            // user-defined terms
            if (name == null && !HPOTerm.isValidID(hpoID)) {
                name = hpoID;
            }

            this._hpoID  = hpoID;
            this._name   = name ? name : "loading...";

            if (!name && callWhenReady)
                this.load(callWhenReady);
        },

        /*
         * Returns the hpoID of the phenotype
         */
        getID: function() {
            return this._hpoID;
        },

        /*
         * Returns the name of the term
         */
        getName: function() {
            return this._name;
        },

        load: function(callWhenReady) {
            var baseServiceURL = editor.getExternalEndpoint().getHPOServiceURL();
            var queryURL       = baseServiceURL + "/" + this._hpoID.replace(":","%3A");
            //console.log("QueryURL: " + queryURL);
            new Ajax.Request(queryURL, {
                method: "GET",
                onSuccess: this.onDataReady.bind(this),
                //onComplete: complete.bind(this)
                onComplete: callWhenReady
            });
        },

        onDataReady : function(response) {
            this._name = this._hpoID;
            try {
                var parsed = JSON.parse(response.responseText);
                //console.log(Helpers.stringifyObject(parsed));
                if (parsed.hasOwnProperty("name")) {
                    console.log("LOADED TERM INFO: id = " + this._hpoID + ", name = " + parsed.name);
                    this._name = parsed.name;
                } else {
                    console.log("LOADED TERM INFO: id = " + this._hpoID + " -> NO DATA");
                }
            } catch (err) {
                console.log("[LOAD TERM] Parse Error: " +  err);
            }
            document.fire('term:name', {'id' : this._hpoID, 'name': this._name});
        }
    });

    HPOTerm.isValidID = function(id) {
        var pattern = /^HP\:(\d)+$/i;
        return pattern.test(id);
    }

    return HPOTerm;
});
