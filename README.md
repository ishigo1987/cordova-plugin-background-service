BackgroundService
==============================

## Installing the plugin ##

```Bash
$ cordova plugin add cordova-plugin-background-service
```

## Using the plugin ##

The plugin creates the object `window.BackgroundService` with the methods `start(success, failure, config)`, and `stop(success, failure)`.

```Javascript
var bgs = window.BackgroundService;
bgs.start(
    function(fn) { dosometing(), fn && fn(); },
    function() { console.log('err'); },
    {
        // Android Only
        title: 'App is running in background',
        text: 'Doing heavy tasks.',
        bigText: false,
        resume: true,
        silent: true,
        hidden: true,
        color: null,
        icon: 'icon',
        // iOS Only
        stopOnTerminate: false // Set true to cease background-fetch from operating after user "closes" the app (Default: true)
    }
);
```
