/**
 * PersonGroupHoverbox is a class for all the UI elements and graphics surrounding a PersonGroup node and
 * its labels. This includes the box that appears around the node when it's hovered by a mouse.
 *
 * @class GroupHoverbox
 * @extends AbstractHoverbox
 * @constructor
 * @param {PersonGroup} node The node PersonGroup for which the hoverbox is drawn
 * @param {Number} centerX The x coordinate for the hoverbox
 * @param {Number} centerY The y coordinate for the hoverbox
 * @param {Raphael.st} nodeShapes Raphaël set containing the graphical elements that make up the node
 */
define([
        "pedigree/pedigreeEditorParameters",
        "pedigree/view/personHoverbox"
    ], function(
        PedigreeEditorParameters,
        PersonHoverbox
    ){
    var PersonGroupHoverbox = Class.create(PersonHoverbox, {
        initialize: function($super, personNode, centerX, centerY, nodeShapes, nodeMenu) {
            $super(personNode, centerX, centerY, nodeShapes, nodeMenu);
        },

        /**
        * Creates the handles used in this hoverbox - overriden to generate no handles 
        *
        * @method generateHandles
        * @return {Raphael.st} A set of handles
        */
        generateHandles: function($super) {
            if (this._currentHandles !== null) return;

            if (PedigreeEditorParameters.attributes.newHandles) {
                // TODO: singling handle for person groups?
            }
            // else: no handles
        },

        _generateDeceasedMenu: function() {
            // we do not generate deceased menu for alive and well in group hoverbox
        },

        /**
         * Returns true if the menu for this node is open
         *
         * @method isMenuToggled
         * @return {Boolean}
         */
        isMenuToggled: function() {
            return this._isMenuToggled;
        },

        /**
         * Shows/hides the menu for this node
         *
         * @method toggleMenu
         */
        toggleMenu: function(isMenuToggled) {
            if (this._justClosedMenu) return;
            this._isMenuToggled = isMenuToggled;
            if(isMenuToggled) {
                this.getNode().getGraphics().unmark();
                var optBBox = this.getBoxOnHover().getBBox();
                var x = optBBox.x2;
                var y = optBBox.y;
                var position = editor.getWorkspace().canvasToDiv(x+5, y);
                this._nodeMenu.show(this.getNode(), position.x, position.y);
            }
        }
    });
    return PersonGroupHoverbox;
});
