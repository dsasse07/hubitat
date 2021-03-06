/**
 * https://github.com/dsasse07/hubitat/blob/main/drivers/virtual-timed-switch/virtualTimedSwitch.groovy
 * Switch event code based on Virtual Switch uDTH Lite driver by sab0276:
 * _________________________________________________________________________________________________________________

 * https://github.com/sab0276/Hubitat/blob/main/virtualSwitchUDTH-Lite.groovy
 * Can be used to integrate other systems/devices into Hubitat via 3rd party platforms like IFTTT, Alexa, Webhooks, etc
 * Alexa Routines need to use Contact Sensors or Motion Sensors for their Triggers
 * so if you need Alexa integration, make sure you enable the Contact or Motion Sensor functions in the preferences
 * Note adding some capabilities like Lock or Door Control may limit where it can be used due to security
 * Idea from Mike Maxwell's SmartThings uDTH: https://community.smartthings.com/t/release-universal-virtual-device-type-and-translator/47836
 * If you need more than just SWITCH, CONTACT, MOTION, and/or PRESENCE, use my Virtual Switch uDTH Super device driver for that device instead.    
 * Force State Update preference will send an event everytime you manually push a form button or app tries to do something with the device.  Ie.  If the device is already On, and an app tries to turn it On, it will send a On/Open/Motion/Present event. 
 *
 *
 *
 * Timer display and management code based on the "Timer Device" driver below created by apwelsh:
 * _________________________________________________________________________________________________________________
 * Timer Device
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a simple count-down timer I created for a friend.  I wanted to use the standard TimedSession capability.
 * Making it work with rules is more difficult than it should be though, since HE does not seem to support this yet.
 * As such, I implement the PushableButton as well to be able to trigger an event when the timer has expired.  This is a
 * very simple timer based on a cron type schedule.  It is now an accurate timer, that is rather light-weight.
 * To use the timer, first set the TimeRemaining attribute, then start the timer.
 *-------------------------------------------------------------------------------------------------------------------
 * Copyright 2020 Armand Peter Welsh
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 * documentation files (the 'Software'), to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, 
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of 
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO 
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *-------------------------------------------------------------------------------------------------------------------
*/

metadata {
    definition (name: "Virtual Timed Switch", namespace: "dsasse07", author: "Daniel Sasse") {
        capability "Switch"		//"on", "off"
        capability "TimedSession"

        attribute 'display', 'string'
 	}   
    
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "forceUpdate", type: "bool", title: "Force State Update", description: "Send event everytime, regardless of current status. ie Send/Do On even if already On.",  defaultValue: false
        input name: "autoOffTimer", type: "number", title: "Enable Auto-Off", description: "Automatically turns off the specified time in seconds.", defaultValue: 0
    } 
}

def off() {
    sendEvent(name: "switch", value: "off", isStateChange: forceUpdate)
    logTxt "turned Off"
    stop()
}

def on() {
    if (autoOffTimer < 1) return
    sendEvent(name: "switch", value: "on", isStateChange: forceUpdate)
    logTxt "turned On"

    setTimeRemaining(autoOffTimer)
    runIn(autoOffTimer, off)

    start()
}

/** 
Timer Methods
*/

def setTimeRemaining(seconds) {

    if (seconds == 0) {
        timerDone()
    }

    if (state.alerttime) {

        if (state.alerttime < now() + (seconds * 1000)) {
            if (logEnable) log.info "Resetting time remaining to ${seconds} seconds"
            unschedule()
            
            runIn(seconds as int, timerDone,[overwrite:false, misfire: 'ignore'])
            state.alerttime = now() + (seconds * 1000)

            state.refreshInterval = 1
            schedule('* * * * * ?', timerEvent, [misfire: 'ignore', overwrite: false])
        } else {
            scheduleTimerEvent(seconds as int)
        }

    }
    def tempTime = seconds
    def hours = (tempTime / 3600) as int
    if (hours > 0)
        tempTime = tempTime.intValue() % 3600 // remove the hours component
    def mins = (tempTime / 60) as int
    def secs = (tempTime.intValue() % 60) as int
    if (hours > 0) {
        remaining = String.format('%d:%02d:%02d', hours, mins, secs)
    } else {
        remaining = String.format('%02d:%02d', mins, secs)
    }
    
    sendEvent(name: 'timeRemaining', value: seconds)
    sendEvent(name: 'display', value: remaining)

}

def scheduleTimerEvent(secondsRemaining) {
    def refreshInterval = 1

    if (secondsRemaining > 60) {
        if ((secondsRemaining as int) % 10 == 0) refreshInterval = 10
        else return
    }
    else if (secondsRemaining > 10) {
        if ((secondsRemaining as int) % 5 == 0)  refreshInterval = 5
        else return
    }
    
    if (((state.refreshInterval?:0) as int) != refreshInterval) {
        def t = refreshInterval == 1 ? '*' : new Date().getSeconds() % refreshInterval
        unschedule(timerEvent)
        schedule("${t}/${refreshInterval} * * * * ?", timerEvent, [misfire: 'ignore', overwrite: false])
        state.refreshInterval = refreshInterval
        if (logEnable) log.info "Changed timer update frequency to every ${refreshInterval} second(s)"
    }
}

def start() {
    if (logEnable) log.info 'Timer started'
    unschedule()
    int timeRemaining = (device.currentValue('timeRemaining') ?: 0 as int)

    setStatus('running')
    
    runIn(timeRemaining, timerDone,[overwrite:false, misfire: 'ignore'])
    state.alerttime = now() + (timeRemaining * 1000)

    def refreshInterval = 1
    state.refreshInterval = refreshInterval
    schedule('* * * * * ?', timerEvent, [misfire: 'ignore', overwrite: false])
}

def stop() {
    unschedule()
    setTimeRemaining(0)
    if (logEnable) log.info 'Timer stopped'
}

/*
def cancel() {
    if (logEnable) log.info 'Canceling timer'
    setStatus('canceled')
    setTimeRemaining(0)
}

def pause() {
    if (state.alerttime) {
        setTimeRemaining(((state.alerttime - now()) / 1000) as int)
        unschedule()
        state.remove('refreshInterval')
        state.remove('alerttime')
        setStatus('paused')
        if (logEnable) log.info 'Timer paused'
    }
}
*/

/**
 ** Support Methods
 **/

def setStatus(status) {
    sendEvent(name: 'sessionStatus', value: status, isStateChange: true)
    switch (status) {
        case 'running':
        case 'paused':
            sendEvent(name: 'switch', value: 'on')
            break;
        default:
            sendEvent(name: 'switch', value: 'off')
    }
}

def resetDisplay() {
    sendEvent(name: 'display', value: idleText ? 'idle' : '--:--')
}

def timerDone() {
    state.remove('alerttime')
    state.remove('refreshInterval')
    unschedule()
    if (device.latestValue('sessionStatus') != 'canceled') {
        sendEvent(name: 'timeRemaining', value: 0)
        setStatus('stopped')
    }
    runIn(1, resetDisplay)
}

def timerEvent() {
    if (state.alerttime) {
        setTimeRemaining(((state.alerttime - now())/1000) as int)
    } else {
        stop()
    }
}

def installed() {
}

void logTxt(String msg) {
	if (logEnable) log.info "${device.displayName} ${msg}"
}

//Use only if you are on 2.2.8.141 or later.  device.deleteCurrentState() is new to that version and will not work on older versions.  
def configure(){  
    logTxt "configured. State values reset."
}