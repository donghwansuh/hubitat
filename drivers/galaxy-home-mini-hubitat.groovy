/*
 * Galaxy Home Mini Hubitat driver (Smartthings API)
 *
 *
 * 2020.12.19 0.1 Initial version
 *
 */
metadata {
    definition(name: "Galaxy Home Mini", namespace: "donghwansuh", author: "DongHwan Suh") {
        capability "SpeechSynthesis"
    }

    command "setVolume", [[name:"Volume", type:"NUMBER"]] // actual applied value is Volume*2/3
    command "playNews"
    command "playWeather"
    command "playTopMusic"
    command "commandBixby", [[name:"Command", type:"STRING"]]

    preferences {
    input name: "deviceid", type: "text", title: "Device ID", required: false
    input name: "token", type: "text", title: "Token", required: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)
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
    log.debug(builder.toString())
    return params
}

def post(params){
    try {
        httpPostJson(params) { resp ->
            //if (resp.success) {
            //}
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def speak(speechText){
    def params = buildParms("speechSynthesis", "speak", speechText)
    post(params)
}

def setVolume(vol_){
    def params = buildParms("audioVolume", "setVolume", vol_)
    post(params)
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