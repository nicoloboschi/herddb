function tablespaceController($scope, $http, $route, $timeout, $location) {
    $scope.go = function (path) {
        $location.path(path);
    };

    $scope.requestTableSpaces = function () {
        var url = "http://localhost:8086/herddb-ui/webresources/api/tablespaces";
        $http.get(url).
                success(function (data, status, headers, config) {
                    var $table = $('#tableTableSpaces');
                    if ($.fn.dataTable.isDataTable($table)) {
                        $table.empty();
                        $table.DataTable().destroy();
                    }
                    console.log(data);
                    var opt = getCommonDatableOptions();
                    opt['data'] = data;
                    var table = $table.DataTable(opt);
                    
                    $('#tableTableSpaces').find('td').click(function () {
                        alert(this);
                        $scope.requestTableSpace('default');
                        $('#divTableSpaces').hide();
                        $('#divTableSpace').fadeIn(500);
                    });
                }).
                error(function (data, status, headers, config) {
                    console.error('error');
                });
    };

    $scope.requestTableSpace = function (ts) {
        var url = "http://localhost:8086/herddb-ui/webresources/api/tablespace?ts=" + encodeURIComponent(ts);
        $scope.actualTableSpace = ts;
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
        $scope.requestTableSpaces();
        selectActiveLiById('home-li');
        $('#divTableSpaceStats').hide();

        $('#tableTableSpaces').find('td').click(function () {
            alert(this);
            requestTableSpaceStat('default');
            $('#divTableSpaces').hide();
            $('#divTableSpaceStats').fadeIn(500);
        });
    });
}
