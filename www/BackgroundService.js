var exec = require("cordova/exec");
var noop = function () { };

var defaults = {
    title: 'App is running in background',
    text: 'Doing heavy tasks.',
    bigText: false,
    resume: true,
    silent: true,
    hidden: true,
    color: null,
    icon: 'icon'
};

var androidMode = {
    start: function (success, failure, config) {
        success = success || noop, failure = failure || noop;
        //设置默认参数
        for (var key in defaults) {
            config.hasOwnProperty(key) && (defaults[key] = config[key]);
        }
        cordova.exec(null, failure, 'BackgroundMode', 'configure', [defaults, false]);
        //启动定时任务
        var callback = function () {
            androidMode.enabled = true;
            androidMode.timer = setInterval(success, 600000);
        };
        cordova.exec(callback, failure, 'BackgroundMode', 'enable', []);
    },
    stop: function (success, failure) {
        androidMode.enabled = false;
        clearInterval(androidMode.timer);
        success = success || noop, failure = failure || noop;
        exec(success, failure, 'BackgroundMode', 'disable', []);
    },
    fire: function (status, params) {
        androidMode.actived = status == 'activate';
        console.log('BackgroundMode:', status, params);
    },
    //其他参数
    timer: null,
    actived: false,
    enabled: false
};

var iosMode = {
    start: function (success, failure, config) {
        success = success || noop, failure = failure || noop;
        var fn = function () {
            success(function () {
                exec(null, null, 'BackgroundFetch', 'finish', []);
            });
        };
        config = { stopOnTerminate: config && config.stopOnTerminate };
        exec(fn, failure, 'BackgroundFetch', 'configure', [config]);
    },
    stop: function (success, failure) {
        success = success || noop;
        failure = failure || noop;
        exec(success, failure, 'BackgroundFetch', 'stop', []);
    }
};

module.exports = cordova.platformId == 'ios' ? iosMode : androidMode;
