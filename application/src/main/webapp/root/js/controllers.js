'use strict';

/* Controllers */

var phonecatControllers = angular.module('phonecatControllers', []);

phonecatControllers.controller('PhoneListCtrl', ['$scope', 'Phone', 'Cart', 'Analytics', function($scope, Phone, Cart, Analytics) {
	var self = this;
	$scope.phones = Phone.query();
	$scope.orderProp = 'age';
	$scope.addToCart = function(phone) {
		Cart.add(phone);
	}
	$scope.cartItems =  Cart.get();
	$scope.removeFromCart = function(cartItem) {
		Cart.remove(cartItem);
	}
	Analytics.fetchLastItems(function(data) {
		$scope.lastItems = data;
	});
  }]);

phonecatControllers.controller('PhoneDetailCtrl', ['$scope', '$routeParams', 'Phone',
  function($scope, $routeParams, Phone) {
    $scope.phone = Phone.get({phoneId: $routeParams.phoneId}, function(phone) {
      $scope.mainImageUrl = phone.images[0];
    });

    $scope.setImage = function(imageUrl) {
      $scope.mainImageUrl = imageUrl;
    }
  }]);



phonecatControllers.controller('CheckoutCtrl', ['$location', '$scope', 'Cart',
  function($location, $scope, Cart) {
	$scope.orderId = '';
    $scope.cartItems =  Cart.get();

    $scope.removeFromCart = function(cartItem) {
		Cart.remove(cartItem);
	}
    $scope.placeOrder = function() {
    	Cart.placeOrder().success(function(orderState) {
    		if(orderState.state === 'OrderProcessed') {
    			$scope.orderId = orderState.orderId;
    			Cart.clearCart();
    		 }
    	});
    }
 }]);
