/*
 * Galaxy Home Mini IR Air Conditioner Hubitat driver (Using Smartthings REST API)
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
 * 2022.10.03 0.1 Initial version
 *
 */
metadata {
    definition(name: "Galaxy Home Mini IR Air Conditioner", namespace: "donghwansuh", author: "Donghwan Suh") {
        capability "Switch"
        capability "ThermostatCoolingSetpoint"

        command "temperatureDown"
        command "temperatureUp"
        command "fanspeedDown"
        command "fanspeedUp"
        command "setMode", [["name": "Mode", "type": "ENUM", "constraints":["auto", "cool", "dry", "fanOnly", "heat"]]]
        command "setFanMode", [["name": "Mode", "type": "ENUM", "constraints":["low", "medium", "high"]]]
        //command "setCoolingSetpoint", [["name":"Temperature*", type:"NUMBER"]]
        command "fetchStatus"

        attribute "switch", "string"
        attribute "mode", "string"
        attribute "fanMode", "string"
        attribute "coolingSetPoint", "NUMBER"
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
            if (resp.success) {
                fetchStatus()
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def on(){
    def params = buildParms("switch", "on")
    post(params)
}

def off(){
    def params = buildParms("switch", "off")
    post(params)
}

def temperatureDown(){
    def params = buildParms("statelessTemperatureButton", "setButton", "temperatureDown")
    post(params)
}

def temperatureUp(){
    def params = buildParms("statelessTemperatureButton", "setButton", "temperatureUp")
    post(params)
}

def fanspeedDown(){
    def params = buildParms("statelessFanspeedButton", "setButton", "fanspeedDown")
    post(params)
}

def fanspeedUp(){
    def params = buildParms("statelessFanspeedButton", "setButton", "fanspeedUp")
    post(params)
}

def setMode(_mode){
    def params = buildParms("airConditionerMode", "setAirConditionerMode", _mode)
    post(params)
}

def setFanMode(_mode){
    def params = buildParms("airConditionerFanMode", "setFanMode", _mode)
    post(params)
}

def setCoolingSetpoint(_temperature){
    def params = buildParms("thermostatCoolingSetpoint", "setCoolingSetpoint", _temperature)
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
            
            sendEvent(name: "switch", value: resp.data.components.main.switch.switch.value)
            sendEvent(name: "mode", value: resp.data.components.main.airConditionerMode.airConditionerMode.value)
            sendEvent(name: "fanMode", value: resp.data.components.main.airConditionerFanMode.fanMode.value)
            sendEvent(name: "coolingSetPoint", value: resp.data.components.main.thermostatCoolingSetpoint.coolingSetpoint.value)
        }
    } catch (e) {
        log.error "$e"
    }
}