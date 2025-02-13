define([
        "pedigree/model/helpers"
    ], function(
        Helpers
    ){

    // order:   1D array of 1D arrays: for each rank list of vertices in order
    //
    // _vOrder: (optional) 1D array: for each nodeID vOrder[nodeID] == order within the rank
    //          _vOrder can be derived from the `order` above, and is provided optionally for performance reasons
    Ordering = function (order, _vOrder) {
        this.order = order;

        if (_vOrder) {
            this.vOrder = _vOrder;
        } else {
            this.vOrder = [];
            this._updateVOrderArray();
        }
    };

    Ordering.createOrdering = function(vOrder, ranks) {
        if (vOrder.length != ranks.length) {
            throw "Can not create ordering since the number of positions and nodes does not match";
        }

        var maxRank = Math.max.apply(null, ranks)
        var order = [];
        for (var r = 0; r <= maxRank; r++) {
            order[r] = [];
        }

        // fill in order 2d array which represents orders-per-rank based on given order-per-vertex 1D array
        for (var i = 0; i < vOrder.length; i++) {
            if (order[ranks[i]][vOrder[i]] !== undefined) {
                throw "Some nodes have the same generation (" +  ranks[i] + ") and same order (" + vOrder[i] + ")";
            }
            order[ranks[i]][vOrder[i]] = i;
        }

        // remove gaps on each rank
        for (var r = 0; r <= maxRank; r++) {
            order[r] = order[r].filter(function(v){return v !== undefined})
        }

        return new Ordering(order);
    };

    Ordering.fromJSON = function(json) {
        return new Ordering(json.order, json.vOrder);
    },

    Ordering.prototype = {

        toJSONObject: function() {
            return JSON.parse(JSON.stringify(this));
        },

        insert: function(rank, insertOrder, vertex) {
           this.order[rank].splice(insertOrder, 0, vertex);
           this.vOrder[vertex] = insertOrder;
           for (var next = insertOrder+1; next < this.order[rank].length; ++next)
               this.vOrder[ this.order[rank][next] ]++;
        },

        exchange: function(rank, index1, index2) {
            // exchanges vertices at two given indices within the same given rank

            var v1 = this.order[rank][index1];
            var v2 = this.order[rank][index2];

            this.order[rank][index2] = v1;
            this.order[rank][index1] = v2;

            this.vOrder[v1] = index2;
            this.vOrder[v2] = index1;
        },

        canMove: function(rank, index, amount) {
            var newIndex = index + amount;
            if (newIndex < 0) return false;
            if (newIndex > this.order[rank].length - 1) return false;
            return true;
        },

        move: function(rank, index, amount) {
            // changes vertex order within the same rank. Moves "amount" positions to the right or to the left
            if (amount == 0) return true;

            var newIndex = index + amount;
            if (newIndex < 0) return false;

            var ord = this.order[rank];
            if (newIndex > ord.length - 1) return false;

            var v = ord[index];

            if (newIndex > index) {
                for (var i = index; i< newIndex; ++i) {
                    var vv          = ord[i+1];
                    ord[i]          = vv;
                    this.vOrder[vv] = i;
                }
            }
            else {
                for (var i = index; i > newIndex; --i) {
                    var vv          = ord[i-1];
                    ord[i]          = vv;
                    this.vOrder[vv] = i;
                }
            }

            ord[newIndex]  = v;
            this.vOrder[v] = newIndex;

            return true;
        },

        copy: function () {
            // returns a deep copy
            return new Ordering(Helpers.clone2DArray(this.order), this.vOrder.slice());
        },

        moveVertexToRankAndOrder: function ( oldRank, oldOrder, newRank, newOrder ) {
            // changes vertex rank and order. Insertion happens right before the node currently occupying the newOrder position on rank newRank
            var v = this.order[oldRank][oldOrder];

            this.order[oldRank].splice(oldOrder, 1);

            this.order[newRank].splice(newOrder, 0, v);

            this.vOrder[v] = newOrder;
            for ( var i = newOrder+1; i < this.order[newRank].length; ++i ) {
                var nextV = this.order[newRank][i];
                this.vOrder[nextV]++;
            }
            for ( var i = oldOrder; i < this.order[oldRank].length; ++i ) {
                var nextV = this.order[oldRank][i];
                this.vOrder[nextV]--;
            }
        },

        moveVertexToOrder: function ( rank, oldOrder, newOrder ) {
            // changes vertex order within the same rank. Insertion happens right before the node currently occupying the newOrder position
            // (i.e. changing order from 3 to 4 does nothing, as before position 4 is still position 3)
            var shiftAmount = (newOrder <= oldOrder) ? (newOrder - oldOrder) : (newOrder - oldOrder - 1);
            this.move( rank, oldOrder, shiftAmount );
        },

        removeUnplugged: function() {
            var result = this.order[0].slice(0); //copy of original unplugged IDs

            for (var u = 0; u < this.order[0].length; ++u) {
                var unplugged = this.order[0][u];

                for (var i = 0; i < this.order.length; ++i)
                    for (var j = 0; j < this.order[i].length; ++j ) {
                        if (this.order[i][j] > unplugged)
                            this.order[i][j]--;
                    }

                    this.vOrder.splice(unplugged, 1);
            }

            this.order[0] = [];

            return result;
        },

        remove: function(v, rank) {
            var order = this.vOrder[v];
            this.moveVertexToRankAndOrder(rank, order, 0, 0);
            this.removeUnplugged();
        },

        insertAndShiftAllIdsAboveVByOne: function ( v, rank, newOrder ) {
            // used when when a new vertex is inserted into the graph, which increases all IDs above v by one
            // so need to modify the data for all existing vertices first, and then insert the new vertex

            for (var i = this.vOrder.length; i > v; --i ) {
                this.vOrder[i] = this.vOrder[i-1];
            }

            for (var i = 0; i < this.order.length; ++i)
                for (var j = 0; j < this.order[i].length; ++j ) {
                    if (this.order[i][j] >= v)
                        this.order[i][j]++;
                }

            this.insert(rank, newOrder, v);
        },

        insertRank: function (insertBeforeRank) {
            this.order.splice(insertBeforeRank, 0, []);
        },

        getRightNeighbour: function(v, rank) {
            var order = this.vOrder[v];
            if ( order < this.order[rank].length-1 )
                return this.order[rank][order+1];
            return null;
        },

        getLeftNeighbour: function(v, rank) {
            var order = this.vOrder[v];
            if ( order > 0 )
                return this.order[rank][order-1];
            return null;
        },

        sortByOrder: function(v_list) {
            var vorders = this.vOrder;
            var result = v_list.slice(0);
            result.sort(function(x, y){ return vorders[x] - vorders[y] });
            return result;
        },

        // returns all vertices ordered from left-to-right and from top-to-bottom
        getLeftToRightTopToBottomOrdering: function(onlyType, GG) {
            var result = [];
            for (var i = 1; i < this.order.length; ++i) {
                for (var j = 0; j < this.order[i].length; ++j) {
                    var v = this.order[i][j];
                    if (!onlyType || GG.type[v] == onlyType)
                        result.push(this.order[i][j]);
                }
            }
            return result;
        },

        // makes a mirror left-right ordering flip
        flipOrders: function() {
            for (var r = 0; r < this.order.length; r++) {
                this.order[r].reverse();
            }
            this._updateVOrderArray();
        },

        _updateVOrderArray: function() {
            // recompute vOrders
            for (var r = 0; r < this.order.length; r++) {
                var ordersAtRank = this.order[r];
                for (var i = 0; i < ordersAtRank.length; i++) {
                    this.vOrder[ordersAtRank[i]] = i;
                }
            }
        }
    };

    return Ordering;
});
