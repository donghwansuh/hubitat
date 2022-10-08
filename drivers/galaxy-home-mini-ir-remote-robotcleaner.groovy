/*
 * Galaxy Home Mini IR Remote Robot Cleaner Hubitat driver (Using Smartthings REST API)
 *
 * Copyright 2022 Donghwan Suh
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
 * 2022.10.08 0.1 Initial version
 *
 */
metadata {
    definition(name: "Galaxy Home Mini IR Remote Robot Cleaner", namespace: "donghwansuh", author: "Donghwan Suh") {
        capability "Actuator"
        
        command "powerToggle"
        command "actionToggle"
        command "home"
        command "partCleaning"
        command "repeatCleaning"
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

def post(params){
    try {
        httpPostJson(params) { resp ->
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def powerToggle(){
    def params = buildParms("statelessPowerToggleButton", "setButton", "powerToggle")
    post(params)
}

def actionToggle(){
    def params = buildParms("statelessRobotCleanerToggleButton", "setButton", "actionToggle")
    post(params)
}

def home(){
    def params = buildParms("statelessRobotCleanerHomeButton", "setButton", "home")
    post(params)
}

def partCleaning(){
    def params = buildParms("statelessCustomButton", "setButton", "partCleaning")
    post(params)
}

def repeatCleaning(){
    def params = buildParms("statelessCustomButton", "setButton", "repeatCleaning")
    post(params)
}
