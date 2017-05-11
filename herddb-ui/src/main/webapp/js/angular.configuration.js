var modulo = angular.module('herddb-ui-module', ['ngRoute']);

modulo.config(function ($routeProvider) {
    $routeProvider.when('/home',
            {templateUrl: 'html/tablespaces.html', controller: tablespaceController});
    $routeProvider.when('/pag2',
            {templateUrl: 'html/nodes.html', controller: nodeController});
    $routeProvider.otherwise({redirectTo: '/home'});
});

function selectActiveLi(li) {
    $('li').removeClass('active');
    $(li).addClass('active');
}
function selectActiveLiById(id) {
    $('li').removeClass('active');
    $('#' + id).addClass('active');
}

function getCommonDatableOptions() {

    return {searching: false, 
        scrollY: '100vh', 
        scrollCollapse: true, 
        paging: false, 
        info: false, 
    }
}