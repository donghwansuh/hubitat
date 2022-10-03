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
 * 2022.10.03 0.1 Initial version
 *                Supports Galaxy Home Mini, Galaxy Home Mini IR Air Conditioner.
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
        section("<b>IMPORTANT</b>") {
            paragraph "The label of each device registered on Smartthings should be unique."
            paragraph "스마트싱스에 등록된 각 디바이스의 이름이 고유해야 합니다."
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
                    String mapKey = "${entry.label} (${entry.deviceId})"
                    String mapKeyLocal = "${entry.label} (st_${entry.deviceId})"
                    if (!state.childList.containsKey(mapKeyLocal)){
                        if (logEnable) log.debug "${mapKey} ${mapKeyLocal}"
                        state.fetchedDevices.put("${mapKey}", entry)
                    }
                }                
            }
        } catch (e) {
            log.error "${e}"
        }
        section
        {
            input name: "devicesToInstall", type: "enum", title: "Select new devices to install to this Hubitat hub.", options: state.fetchedDevices.keySet(), required: false, multiple: true
        }
    }
}

def devicesRemovalPage() {
    dynamicPage(name: "devicesRemovalPage", title: "", install: true, uninstall: false) {
        def children = getChildDevices()
        Map childrenMap = [:]
        children.each { entry ->
            String dnid = entry.getDeviceNetworkId()
            childrenMap.put("${entry.label} (${dnid})", dnid)
        }
        section("<b>Settings</b>") {
            input name: "devicesToRemove", type: "enum", title: "Select devices to remove from this Hubitat Hub.", options: childrenMap.keySet(), required: false, multiple: true
        }
    }
}

def settingsPage() {
    dynamicPage(name: "settingsPage", title: "", install: false, uninstall: false) {
        section("<b>Settings</b>") {
            label title: "Change the name of this app.", required: false
            input name: "alsoDeleteChildren", type: "bool", title: "Deleted child devices when this app is removed", defaultValue: false
            input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
            href page: "mainPage", title: "Go back to the Main page"
        }
    }
}

def installed() {
    devicesToInstall.each { entry ->
        if (logEnable) log.debug "${state.fetchedDevices}.${entry}"
        String stDeviceLabel = state.fetchedDevices."${entry}".label
        String stDeviceID = state.fetchedDevices."${entry}".deviceId
        String deviceNetworkID = "st_" + stDeviceID
        def child = getChildDevice(deviceNetworkID)
        if (logEnable) log.debug "Retrieved Child : ${child}"
        if (child == null){
            String deviceDriverName
            if (state.fetchedDevices."${entry}".type == "OCF"){
                deviceDriverName = "Galaxy Home Mini"
            }
            else if (state.fetchedDevices."${entry}".type == "IR_OCF"){
                deviceDriverName = "Galaxy Home Mini IR Air Conditioner"
            }
            try {
                def childDevice = addChildDevice("donghwansuh", deviceDriverName, deviceNetworkID, [name: deviceDriverName, label: stDeviceLabel, isComponent: false])
                childDevice.updateSetting("deviceid", stDeviceID)
                childDevice.updateSetting("token", personalAccessToken)
                log.info "Installed : ${entry} (${stDeviceID})"
            } catch (e) {
                log.error "${e}"
            }
        }
        else log.info "Device already exists : ${entry} (${stDeviceID})"  
    }
    app.clearSetting("devicesToInstall")
    
    devicesToRemove.each { entry ->
        log.debug "${state.childList.getAt(entry)}"
        deleteChildDevice(state.childList.getAt(entry))
    }
    app.clearSetting("devicesToRemove")

    state.childList.clear()
    state.fetchedDevices.clear()
}

def updated() {
    installed()
}

def uninstalled()
{
    if (alsoDeleteChildren){
        deleteChildrenD(getChildDevices())
    }
}

def deleteChildrenD(_children){
    _children.each { entry->
        deleteChildDevice(entry.getDeviceNetworkId())
    }
}