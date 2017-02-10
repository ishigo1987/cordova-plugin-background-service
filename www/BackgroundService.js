var exec = require('cordova/exec');

var noop = function () { }, timer = null;

var androidMode = {
    enabled: false,
    start: function (success, failure) {
        success = success || noop;
        failure = failure || noop;
        var callback = function () {
            androidMode.enabled = true;
            timer && clearInterval(timer);
            timer = setInterval(success, 600000);
        };
        cordova.exec(callback, failure, 'BackgroundMode', 'enable', []);
    },
    stop: function (success, failure) {
        success = success || noop;
        failure = failure || noop;
        androidMode.enabled = false;
        timer && clearInterval(timer);
        exec(success, failure, 'BackgroundMode', 'disable', []);
    },
    actived: false,
    fire: function (status, params) {
        console.log('BackgroundMode:', status, params);
        androidMode.actived = status == 'activate';
    }
};

var iosMode = {
    enabled: false,
    start: function (success, failure) {
        iosMode.enabled = true;
        success = success || noop;
        failure = failure || noop;
        var callback = function () {
            success(function () {
                exec(null, null, 'BackgroundFetch', 'finish', []);
            });
        };
        exec(callback, failure, 'BackgroundFetch', 'configure', [{ stopOnTerminate: false }]);
    },
    stop: function (success, failure) {
        iosMode.enabled = false;
        success = success || noop;
        failure = failure || noop;
        exec(success, failure, 'BackgroundFetch', 'stop', []);
    }
};

module.exports = cordova.platformId == 'ios' ? iosMode : androidMode;
