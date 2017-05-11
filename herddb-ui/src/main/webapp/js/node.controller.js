function nodeController($scope, $http, $route, $timeout, $location) {
    $scope.go = function (path) {
        $location.path(path);
    };

   
    $scope.reloadData = function () {
        var url = "http://localhost:8086/herddb-ui/webresources/api/nodes";
        $http.get(url).
                success(function (data, status, headers, config) {
                    var $table = $('#table');
                    if ($.fn.dataTable.isDataTable($table)) {
                        $table.empty();
                        $table.DataTable().destroy();
                    }
                    console.log(data);
                    var opt = getCommonDatableOptions();
                    opt['data'] = data;
                    var table = $table.DataTable(opt);
                }).
                error(function (data, status, headers, config) {
                    console.error('error');
                });
    };
    
    $(document).ready(function () {
        $scope.reloadData();
        selectActiveLiById('pag2-li');
    });


}
