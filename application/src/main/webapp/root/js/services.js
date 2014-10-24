'use strict';

/* Services */

var phonecatServices = angular.module('phonecatServices', [ 'ngResource' ]);

phonecatServices.factory('Phone', [ '$resource', function($resource) {
	return $resource('phones/:phoneId.json', {}, {
		query : {
			method : 'GET',
			params : {
				phoneId : 'phones'
			},
			isArray : true
		}
	});
} ]).factory('Cart', ['$resource', '$http', '$q', function ($resource, $http, $q) {
	var cartItems = [];
	var orderId = '';
	
    var sendFailure = function (err) {
          console.log('Rejecting');
          return $q.reject(err.data);
      };
  	var initCartItems = function() {
		$http.get('/cart').success(function(data){
			cartItems.length = 0;
		    //see: http://stackoverflow.com/questions/20966878/angularjs-bind-service-array-variable-to-controller-scope
			//adds the items to the original array, assigning the original array the new reference
			//would not be seen by angular
			cartItems.push.apply(cartItems, data);
		}).error(sendFailure);
	}

   var updateCart = function(phone) {
		var existing = false;
		for (var i = 0; i < cartItems.length; i++) {
			if (cartItems[i].item.id == phone.id) {
				cartItems[i].count = cartItems[i].count + 1;
				existing = true;
				break;
			}
		}
		if (!existing) {
			var cartItem = {item:phone, count:1};
			cartItems.push(cartItem);
		}
	}
	var removeFromCart = function(cartItem) {
		var index = cartItems.indexOf(cartItem);
		if (index > -1) {
			cartItems.splice(index, 1);
		}
	}
	var clearCart = function() {
		while(cartItems.length) {
			cartItems.pop();
		}
	}
	
	initCartItems();
	return {
		get : function() {
			return cartItems;
		},
		add : function(phone) {
			 return $http.post('/cart', {itemId: phone.id})
			 				.then((function(resp){updateCart(phone)}), sendFailure);
		},
		remove : function(cartItem) {
			 return $http.delete('/cart?itemId='+cartItem.item.id)
				.then((function(resp){removeFromCart(cartItem)}), sendFailure);
		},
		placeOrder :function() {
			 return $http.put('/order', {});
		},
		clearCart: function() {
			clearCart();
		}
	}
} ]).factory('Analytics', ['$http', '$interval', function ($http, $interval) {
	return {
		fetchLastItems : function(processLast10Items) {
			$interval(function() {
				$http.get('/last10items').success(function(data){
					processLast10Items(data);
				});
			}, 5000);
		}
	}
} ]);
