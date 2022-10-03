/*
 * Galaxy Home Mini Hubitat driver (Using Smartthings REST API)
 *
 * Copyright 2020-2022 Donghwan Suh
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 2022.10.03 0.2 Renamed the driver file name from galaxy-home-mini-hubitat.groovy to galaxy-home-mini.groovy
                  Added license information
                  Added volume, audioMute, playbackStatus, mediaPlakbackShuffle, networkAudioTrackData, firmwareVersion attribute
                  Added 'play', 'pause', 'stop', 'announce', 'fetchStatus' methods.
                  Fixed error when calling 'speak' method with extra parameters.
                  Mark parameters of commands, commandBixby, setVolume, and announce, as required.
 * 2020.12.19 0.1 Initial version
 *
 */
metadata {
    definition(name: "Galaxy Home Mini", namespace: "donghwansuh", author: "Donghwan Suh") {
        capability "SpeechSynthesis"

        command "setVolume", [[name:"Volume*", type:"NUMBER"]]
        command "volumeDown"
        command "volumeUp"
        command "mute"
        command "unmute"
        command "playNews"
        command "playWeather"
        command "playTopMusic"
        command "commandBixby", [[name:"Command*", type:"STRING"]]
        command "fetchStatus"
        command "play"
        command "pause"
        command "stop"
        //command "setRepeat" , [["name": "Mode", "type": "ENUM", "constraints":["all", "off", "one"]]] //not working
        command "announce", 
             [
                [
                     "name":"Sound",
                     "type":"ENUM",
                     "constraints":["WakeUp", "Breakfast", "Lunch", "Dinner", "TimeToLeave", "MovieTime", "TvTime", "BedTime", "none"]
                ],
                [
                     "name":"Text*",
                     "type":"STRING",
                ]
             ]

        attribute "volume", "number"
        attribute "volume_lastSent", "number"
        attribute "audioMute", "string"
        attribute "playbackStatus", "string"
        attribute "mediaPlaybackShuffle", "string"
        attribute "networkAudioTrackData", "string"
        attribute "firmwareVersion", "string"
    }

    preferences {
    input name: "deviceid", type: "text", title: "Device ID", required: true, defaultValue: "Enter the device ID"
    input name: "token", type: "text", title: "Token", required: true, defaultValue: "Enter your Smartthings Personal Access Token"
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    }
}

def updated() {
    unschedule()
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def parse(String description) {
    if (logEnable) log.debug(description)
}

def buildParms(String capability_, String command_, arg_ = ""){
    def builder = new groovy.json.JsonBuilder()
    def c
    
    if (arg_ instanceof List){
        c = [component:"main", capability:capability_, command:command_, arguments:arg_.collect()]
    }
    else if(arg_ != ""){
        def d = [arg_]
        c = [component:"main", capability:capability_, command:command_, arguments:d.collect()]
    }
    else{
        c = [component:"main", capability:capability_, command:command_]
    }
    
    builder commands:[c]

    def params = [
        uri: "https://api.smartthings.com/v1/devices/" + settings.deviceid + "/commands",
        headers: ['Authorization' : "Bearer " + settings.token],
        body: builder.toString()
    ]
    if (logEnable) log.debug(builder.toString())
    return params
}

boolean post(params){
    boolean result = false
    try {
        httpPostJson(params) { resp ->
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
            if (resp.success) {
                result = true
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
    return result
}

def speak(speechText){
    def params = buildParms("speechSynthesis", "speak", speechText)
    post(params)
}

def speak(speechText, _volume){
    // Can't determine when the speech will end, so the driver can't restore volume to the previous state at the appropriate moment.
    // fetchStatus()
    // def volumePrev = device.currentValue("volume")
    if (_volume != null)
        setVolume(_volume)
    def params = buildParms("speechSynthesis", "speak", speechText)
    post(params)
    // setVolume(volumePrev)
}

def speak(speechText, arg1, arg2){
    if (arg1 != null)
        setVolume(arg1)
    def params = buildParms("speechSynthesis", "speak", speechText)
    post(params)
}

def setVolume(vol_){
    sendEvent(name: "volume_lastSent", value: vol_)
    def params = buildParms("audioVolume", "setVolume", vol_)
    if (post(params))
        fetchStatus()
}

def volumeDown(){
    def params = buildParms("audioVolume", "volumeDown")
    if (post(params))
        fetchStatus()
}

def volumeUp(){
    def params = buildParms("audioVolume", "volumeUp")
    if (post(params))
        fetchStatus()
}


def mute(){
    def params = buildParms("audioMute", "mute")
    if (post(params))
        fetchStatus()
}

def unmute(){
    def params = buildParms("audioMute", "unmute")
    if (post(params))
        fetchStatus()
}

def playNews(){
    def params = buildParms("samsungim.bixbyContent", "bixbyCommand", "news")
    post(params)
}

def playWeather(){
    def params = buildParms("samsungim.bixbyContent", "bixbyCommand", "weather")
    post(params)    
}

def playTopMusic(){
    def params = buildParms("samsungim.bixbyContent", "bixbyCommand", "music")
    post(params)    
}

def commandBixby(commandStr_){
    def params = buildParms("samsungim.bixbyContent", "bixbyCommand", ["search_all", commandStr_])
    post(params)
}

def play(){
    def params = buildParms("mediaPlayback", "play")
    post(params)
}

def pause(){
    def params = buildParms("mediaPlayback", "pause")
    post(params)
}

def stop(){
    def params = buildParms("mediaPlayback", "stop")
    post(params)
}

// def setRepeat(_mode){
//     def params = buildParms("mediaPlaybackRepeat", "setPlaybackRepeatMode", _mode)
//     post(params)
// }

def announce(_sound, _text){
    def params = buildParms("samsungim.announcement", "announce", ["text", "text/plain", "", _sound, _text])
    post(params)
}

def fetchStatus(){
    def params = [
        uri: "https://api.smartthings.com/v1/devices/" + settings.deviceid + "/status",
        headers: ['Authorization' : "Bearer " + settings.token],
    ]

    try {
        httpGet(params) { resp ->
            if (logEnable){
                resp.headers.each {
                    log.debug "${it.name} : ${it.value}"
                }
                log.debug "response contentType: ${resp.contentType}"
                log.debug "response data: ${resp.data}"
            }
            
            sendEvent(name: "volume", value: resp.data.components.main.audioVolume.volume.value)
            sendEvent(name: "audioMute", value: resp.data.components.main.audioMute.mute.value)
            sendEvent(name: "playbackStatus", value: resp.data.components.main.mediaPlayback.playbackStatus.value)
            sendEvent(name: "mediaPlaybackShuffle", value: resp.data.components.main.mediaPlaybackShuffle.playbackShuffle.value)
            sendEvent(name: "networkAudioTrackData", value: resp.data.components.main."samsungim.networkAudioTrackData".appName.value)
            sendEvent(name: "firmwareVersion", value: resp.data.components.main.ocf.mnfv.value)
            
        }
    } catch (e) {
        log.error "$e"
    }
}