/*
 * Smartthings REST API Devices Manager for Hubitat
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
 * 2022.10.08 0.2 Fixed logging.
                  Refactored code.
                  Added supported of device types:
                  Galaxy Home Mini IR Remote Fan, Galaxy Home Mini IR Remote Air Purifier,
                  Galaxy Home Mini IR Remote Wall Switch, Galaxy Home Mini IR Remote Robot Cleaner
 * 2022.10.03 0.1 Initial version
 *                Supports Galaxy Home Mini, Galaxy Home Mini IR Remote Air Conditioner.
 *
 */
definition(
    name: "Smartthings REST API Devices Manager",
    namespace: "donghwansuh",
    author: "Donghwan Suh",
    description: "",
    category: "",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences{
    page(name: "mainPage")
    page(name: "devicesPage")
    page(name: "devicesRemovalPage")
    page(name: "settingsPage")
}

import groovy.transform.Field
@Field static final Map ST_DEV_PID =["IM-SPEAKER-AI-0001" : "Galaxy Home Mini|donghwansuh",
                                    "SmartThings-smartthings-IR_Remote_Air_Conditioner":"Galaxy Home Mini IR Remote Air Conditioner|donghwansuh",
                                    "SmartThings-smartthings-IR_Remote_Fan":"Galaxy Home Mini IR Remote Fan|donghwansuh",
                                    "SmartThings-smartthings-IR_Remote_Air_Purifier":"Galaxy Home Mini IR Remote Air Purifier|donghwansuh",
                                    "SmartThings-smartthings-IR_Remote_Wall_Switch":"Galaxy Home Mini IR Remote Wall Switch|donghwansuh",
                                    "SmartThings-smartthings-IR_Remote_Robot_Cleaner":"Galaxy Home Mini IR Remote Robot Cleaner|donghwansuh"]

def mainPage() {
    state.childList = [:]
    def children = getChildDevices()
    children.each { entry->
        String dnid = entry.getDeviceNetworkId();
        String mapKeyLocal = "${entry.label} (${dnid})"
        state.childList.put(mapKeyLocal , dnid)
    }

    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        section("<b>Smartthings REST API Devices Manager</b>") {
            input name: "personalAccessToken", type: "string", title: "Smartthings Personal Access Token", required: true
            paragraph "<a href='https://account.smartthings.com/tokens' target='_blank'>Create or manage tokens</a>"
        }
        section("<b>Settings</b>") {
            href page: "devicesPage", title: "<b>Add new devices</b>", description: ""
            href page: "devicesRemovalPage", title: "<b>Remove installed devices</b>", description: ""
            href page: "settingsPage", title: "<b>Change settings of this app</b>", description: ""
        }
    }
}

def devicesPage() {
    dynamicPage(name: "devicesPage", title: "", install: true, uninstall: true) {
        state.fetchedDevices = [:]
        def httpParams = [
        uri: "https://api.smartthings.com/v1/devices/",
        headers: ['Authorization' : "Bearer " + personalAccessToken],
        ]
        def sortedList = []
        try {
            httpGet(httpParams) { resp ->
                if (logEnable){
                    resp.headers.each {
                        log.debug "${it.name} : ${it.value}"
                    }
                    log.debug "response contentType: ${resp.contentType}"
                    log.debug "response data: ${resp.data}"
                }

                resp.data.items.each { entry ->
                    String hubDriverMetadata = ST_DEV_PID.getAt(entry.presentationId)
                    if (!hubDriverMetadata){
                        if (logEnable) log.warn "Not yet implemented - presentationID : ${entry.presentationId}"
                        return
                    }
                    String mapKey = "${entry.label} (${entry.deviceId})"
                    String mapKeyLocal = "${entry.label} (st_${entry.deviceId})"
                    if (!state.childList.containsKey(mapKeyLocal)){
                        if (logEnable) log.debug "${mapKey} ${mapKeyLocal}"
                        state.fetchedDevices.put("${mapKey}", entry)
                        sortedList.add(mapKey)
                    }
                }                
            }
        } catch (e) {
            log.error "${e}"
        }
        section {
            input name: "devicesToInstall", type: "enum", title: "Select new devices to install to this Hubitat hub.", options: sortedList.sort(), required: false, multiple: true
        }
    }
}

def devicesRemovalPage() {
    dynamicPage(name: "devicesRemovalPage", title: "", install: true, uninstall: false) {
        def children = getChildDevices()
        Map childrenMap = [:]
        def sortedList = []
        children.each { entry ->
            String dnid = entry.getDeviceNetworkId()
            childrenMap.put("${entry.label} (${dnid})", dnid)
            sortedList.add("${entry.label} (${dnid})")
        }
        
        section {
            input name: "devicesToRemove", type: "enum", title: "Select devices to remove from this Hubitat Hub.", options: sortedList.sort(), required: false, multiple: true
        }
    }
}

def settingsPage() {
    dynamicPage(name: "settingsPage", title: "", install: false, uninstall: false) {
        section {
            label title: "Change the name of this app.", required: false
            input name: "alsoDeleteChildren", type: "bool", title: "Remove child devices when this app is removed", defaultValue: false
            input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        }
    }
}

def installed() {
    devicesToInstall.each { entry ->
        if (logEnable) log.debug "${entry} : " + state.fetchedDevices."${entry}"

        String stDeviceLabel = state.fetchedDevices."${entry}".label
        String stDeviceID = state.fetchedDevices."${entry}".deviceId
        String stDevicePresentationId = state.fetchedDevices."${entry}".presentationId
        String deviceNetworkID = "st_" + stDeviceID
        if (logEnable) log.debug stDeviceID + " " + stDevicePresentationId
        def child = getChildDevice(deviceNetworkID)
        if (logEnable) log.debug "Retrieved Child : ${child}"

        if (child == null){
            String hubDriverMetadata = ST_DEV_PID.getAt(stDevicePresentationId)
            def (hubDeviceDriverName, hubNamespace) = hubDriverMetadata.tokenize('|')
            try {
                def childDevice = addChildDevice(hubNamespace, hubDeviceDriverName, deviceNetworkID, [name: deviceDriverName, label: stDeviceLabel, isComponent: false])
                childDevice.updateSetting("deviceid", stDeviceID)
                log.info "Installed : ${entry}"
            } catch (e) {
                log.error "${e}"
            }
        }
        else log.warn "Device already exists : ${entry}"  
    }
    app.clearSetting("devicesToInstall")
    
    devicesToRemove.each { entry ->
        if (logEnable) log.debug "${state.childList.getAt(entry)}"
        deleteChildDevice(state.childList.getAt(entry))
        log.info "Removed : ${entry}"
    }
    app.clearSetting("devicesToRemove")

    def children = getChildDevices()
        children.each { entry->
        entry.updateSetting("token", personalAccessToken)
    }

    state.childList.clear()
    state.fetchedDevices.clear()
}

def updated() {
    installed()
}

def uninstalled()
{
    if (alsoDeleteChildren){
        def children = getChildDevices()
        children.each { entry->
        deleteChildDevice(entry.getDeviceNetworkId())
        }
    }
}
