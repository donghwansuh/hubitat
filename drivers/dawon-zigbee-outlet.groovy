/**
 *  Copyright 2019 SmartThings
 *  Copyright 2020-2023 Donghwan Suh
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *  DAWON DNS Zigbee Outlet driver for Hubitat
 *
 *  2023/01/24 v0.5 Temperature reporting preferences are only shown when used with a supported device.
                    Added setReporting command which is used for enabling or disabling reporting.
                    Added setReportingParams command which is used for setting reporting parameters.
 *  2022/10/01 v0.4 Renamed the driver file name from DawonOutlet.groovy to dawon-zigbee-outlet.groovy
                    Removed DawonOutlet_Temperature.groovy
                    Removed monthly accumulated data reset function.
                    Added option to show description regarding the usage of this driver.
                    Renamed the driver's name to 'Dawon Zigbee Outlet'.
 *  2021/11/22 v0.3 Merged temperature measurement function along with identification of temperature measurement capable device model.
                    Added support for disabling reporting of power, energy, and temperature from the device and adjusting intervals of reporting (only tested working on PM-B540-ZB).
 *  2021/01/23 v0.2 removed unused codes. (unused Zigbee clusters and checkInterval)
 *  2020/11/24 v0.1 initial version
 *
 *  Modified by Donghwan Suh  
 *  Based on following codes
 *  SmartThings https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/devicetypes/smartthings/zigbee-metering-plug.src/zigbee-metering-plug.groovy
 *  Dawon DNS https://pmshop.co.kr/board/free/read.html?no=5552&board_no=2
 *
 */
import hubitat.zigbee.zcl.DataType
import groovy.transform.Field

metadata {
    definition (name: "Dawon Zigbee Outlet", namespace: "donghwansuh", author: "SmartThings/DongHwan Suh") {
        capability "EnergyMeter"
        capability "PowerMeter"
        capability "Actuator"
        capability "Switch"
        capability "Refresh"
        capability "HealthCheck"
        capability "Sensor"
        capability "Configuration"
        capability "TemperatureMeasurement"

        command "setReporting",
                [["name": "attribute", "type": "ENUM", "constraints":["Power", "Energy", "Temperature"]],
                ["name": "value", "type": "ENUM", "constraints":["Enabled", "Disabled"]]]
        command "setReportingParams",
                [["name": "attribute", "type": "ENUM", "constraints":["PowerIntervalMin", "PowerIntervalMax", "PowerDifference", "EnergyIntervalMin", "EnergyIntervalMax", "EnergyDifference", "TemperatureIntervalMin", "TemperatureIntervalMax", "TemperatureDifference"]],
                ["name": "value", "type": "NUMBER"]]
        command "reset"

        fingerprint endpointId: "01", profileId: "0104", inClusters: "0000, 0003, 0004, 0006, 0019, 0702, 0B04", outClusters: "0000, 0003, 0004, 0006, 0019, 0702, 0B04", manufacturer: "DAWON_DNS", model: "PM-B430-ZB", deviceJoinName: "Dawon Outlet" // DAWON DNS Smart Plug PM-B430-ZB (10A), raw description: 01 0104 0051 01 07 0000, 0004, 0003, 0006, 0019, 0702, 0B04 07 0000, 0004, 0003, 0006, 0019, 0702, 0B04
        fingerprint endpointId: "01", profileId: "0104", inClusters: "0000, 0002, 0003, 0004, 0006, 0019, 0702, 0B04, 0009", outClusters: "0000, 0002, 0003, 0004, 0006, 0019, 0702, 0B04, 0009", manufacturer: "DAWON_DNS", model: "PM-B530-ZB", deviceJoinName: "Dawon Outlet" // DAWON DNS Smart Plug PM-B530-ZB (16A), raw description: 01 0104 0051 01 07 0000, 0004, 0003, 0006, 0019, 0702, 0B04 07 0000, 0004, 0003, 0006, 0019, 0702, 0B04
        fingerprint manufacturer: "DAWON_DNS", model: "PM-C140-ZB", deviceJoinName: "Dawon Outlet" // DAWON DNS In-Wall Outlet PM-C140-ZB, raw description: 01 0104 0051 01 0A 0000 0002 0003 0004 0006 0019 0702 0B04 0008 0009 0A 0000 0002 0003 0004 0006 0019 0702 0B04 0008 0009
        fingerprint profileId: "0104", inClusters: "0000, 0002, 0003, 0006, 0702, 0B04", outClusters: "0003, 0019", manufacturer: "DAWON_DNS", model: "PM-B540-ZB",  deviceJoinName: "Dawon Outlet" // DAWON DNS Smart Plug
        fingerprint profileId: "0104", inClusters: "0000, 0002, 0003, 0006, 0702, 0B04", outClusters: "0003, 0019", manufacturer: "DAWON_DNS", model: "ST-B550-ZB",  deviceJoinName: "Dawon Outlet" // DAWON DNS Smart Plug
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0006, 0702, 0B04", outClusters: "0003, 0019", manufacturer: "DAWON_DNS", model: "PM-C150-ZB",  deviceJoinName: "Dawon Outlet" // DAWON DNS In-Wall Outlet
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0006, 0702, 0B04", outClusters: "0003, 0019", manufacturer: "DAWON_DNS", model: "PM-C250-ZB",  deviceJoinName: "Dawon Outlet" // DAWON DNS In-Wall Outlet
	}
    preferences {
        
        input name: "reportPower", type:"bool", title:"전력량 리포팅 (Enable power reporting)", description: "", required: true, defaultValue: true
        input name: "reportIntervalPowerMin", type: "number", title: "전력량 리포팅 최소 시간 간격 (Minimum interval of power reporting)", range: "1..65534", required: true, defaultValue: 1
        input name: "reportIntervalPowerMax", type: "number", title: "전력량 리포팅 최대 시간 간격 (Maximum interval of power reporting)", range: "1..65534", required: true, defaultValue: 600
        input name: "reportDifferencePower", type: "number", title: "전력량 리포팅을 위한 최소 변동량 (Difference required to report power)", range: "1..65534", required: true, defaultValue: 1
        input name: "reportEnergy", type:"bool", title:"누적 사용량 리포팅 (Enable energy reporting)", description: "", required: true, defaultValue: true
        input name: "reportIntervalEnergyMin", type: "number", title: "누적 사용량 리포팅 최소 시간 간격 (Minimum interval of energy reporting)", range: "1..65534", required: true, defaultValue: 1
        input name: "reportIntervalEnergyMax", type: "number", title: "누적 사용량 리포팅 최대 시간 간격 (Maximum interval of energy reporting)", range: "1..65534", required: true, defaultValue: 600
        input name: "reportDifferenceEnergy", type: "number", title: "누적 사용량 리포팅을 위한 최소 변동량 (Difference required to report energy)", range: "1..65534", required: true, defaultValue: 1
        if (isTemperatureSupported()){
            input name: "reportTemperature", type: "bool", title: "온도 리포팅 (Enable temperature reporting)", description: "", required: true, defaultValue: true
            input name: "reportIntervalTemperatureMin", type: "number", title: "온도 리포팅 최소 시간 간격 (Minimum interval of temperature reporting)", range: "1..65534", required: true, defaultValue: 1
            input name: "reportIntervalTemperatureMax", type: "number", title: "온도 리포팅 최대 시간 간격 (Maximum interval of temperature reporting)", range: "1..65534", required: true, defaultValue: 600
            input name: "reportDifferenceTemperature", type: "number", title: "온도 리포팅을 위한 최소 변동량 (Difference required to report temperature)", range: "1..65534", required: true, defaultValue: 1
        }
        input name: "logEnable", type: "bool", title: "디버그 로그 (Enable debug logging)", defaultValue: false
        input name: "descriptionEnable", type: "bool", title: "드라이버 설명서 표시 (Display the manual of the driver)", defaultValue: true
    }
}

@Field static final String DESC_KR = "(1) 내부 온도 측정은 PM-B540-ZB 모델만 지원합니다. (2) PM-B540-ZB 이외의 모델은 리포팅 설정이 적용되지 않을 수 있습니다. (3) 플러그 자체의 전원이 재인가된 경우나 'reset' 명령을 사용한 경우 리포팅 설정이 초기화 되므로 'congfigure' 명령을 호출해야 리포팅 설정이 적용됩니다. (4) 하단 Preferences에서 설정 변경시 save 후 'configure'를 호출해야 설정이 적용됩니다. (5) 'reset' 호출 시 연결된 장치의 전원이 순간적으로 꺼질 수도 있습니다."
@Field static final String DESC_EN = "(1) Temperature reporting is only supported on PM-B540-ZB. (2) Reporting configuration might not work on models other than PM-B540-ZB. (3) Since the reporting configuration of the outlet resets when the outlet itself was power-cycled or the 'reset' command was called, calling 'configure' is required to set the reporting settings for those situations. (4) Calling 'configure' is required after saving new settings from 'Preferences' below. (5) When 'reset' is called, the connected device may be turned off for a short moment."

def isTemperatureSupported(){return getDataValue("model") == "PM-B540-ZB"}

def installed() {
    state.clear()
    if (isTemperatureSupported() == false)
        device.updateSetting("reportTemperature", false);
    state.DescriptionKR = DESC_KR
    state.DescriptionEN = DESC_EN
    configure()
}

def updated() {
    state.clear()
    if (isTemperatureSupported() == false)
        device.updateSetting("reportTemperature", false);
    if (descriptionEnable) { 
        state.DescriptionKR = DESC_KR
        state.DescriptionEN = DESC_EN       
    }
    log.info "updated. Please re-configure the device to apply changes."
}

def parse(String description) {
    if (logEnable) log.debug "description is $description"
    def event = zigbee.getEvent(description)
    def descMap = zigbee.parseDescriptionAsMap(description)

    if (event) {
        log.info "event enter:$event"
        if (event.name == "switch" && !descMap.isClusterSpecific && descMap.commandInt == 0x0B) {
            log.info "Ignoring default response with desc map: $descMap"
            return [:]
        } else if (event.name== "power") {
            event.value = event.value/getPowerDiv()
            event.unit = "W"
        } else if (event.name== "energy") {
            event.value = event.value/getEnergyDiv()
            event.unit = "kWh"
        }
        //log.info "event outer:$event"
        sendEvent(event)
    } else {
        List result = []
        if (logEnable) log.info "Desc Map: $descMap"

        List attrData = [[clusterInt: descMap.clusterInt ,attrInt: descMap.attrInt, value: descMap.value]]
        descMap.additionalAttrs.each {
            attrData << [clusterInt: descMap.clusterInt, attrInt: it.attrInt, value: it.value]
        }

        attrData.each {
                def map = [:]
                if (it.value && it.clusterInt == 0x0702 && it.attrInt == 0x0400) {
                        if (logEnable) log.debug "power"
                        map.name = "power"
                        map.value = zigbee.convertHexToInt(it.value)/getPowerDiv()
                        map.unit = "W"
                }
                else if (it.value && it.clusterInt == 0x0702 && it.attrInt == 0x0000) {
                        if (logEnable) log.debug "energy"
                        map.name = "energy"
                        map.value = zigbee.convertHexToInt(it.value)/getEnergyDiv()
                        map.unit = "kWh"
                }
                else if (it.value && it.clusterInt == 0x0002 && it.attrInt == 0x0000) {
                        if (logEnable) log.debug "temperature"
                        map.name = "temperature"
                        map.value = zigbee.convertHexToInt(it.value)
                        map.unit = "°C"
                }

                if (map) {
                        result << createEvent(map)
                }
                if (logEnable) log.debug "Parse returned $map"
        }
        return result
    }
}

def off() {
    def cmds = zigbee.off()
    return cmds
}

def on() {
    def cmds = zigbee.on()
    return cmds
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
    return refresh()
}

def refresh() {
    if (logEnable) log.debug "refresh"
    def cmds = zigbee.onOffRefresh() +
    zigbee.readAttribute(0x0702, 0x0000) +
    zigbee.readAttribute(0x0702, 0x0400)
    if (isTemperatureSupported()){
       cmds += zigbee.readAttribute(0x0002, 0x0000)
    }
    return cmds
}

def configure() {
    unschedule()
    if (logEnable) log.debug "Configuring Reporting"
    def cmds = refresh() +
    	   zigbee.onOffConfig()

    int reportIntervalPowerMin_ = (int)reportIntervalPowerMin
    int reportIntervalPowerMax_ = (int)reportIntervalPowerMax
    int reportDifferencePower_ = (int)reportDifferencePower
    int reportIntervalEnergyMin_ = (int)reportIntervalEnergyMin
    int reportIntervalEnergyMax_ = (int)reportIntervalEnergyMax
    int reportDifferenceEnergy_ = (int)reportDifferenceEnergy
    int reportIntervalTemperatureMin_ = (int)reportIntervalTemperatureMin
    int reportIntervalTemperatureMax_ = (int)reportIntervalTemperatureMax
    int reportDifferenceTemperature_ = (int)reportDifferenceTemperature

    if (reportPower){
        if (logEnable) log.debug("ENABLE Power Reporting")
        cmds += zigbee.configureReporting(0x0702, 0x0400, DataType.INT24,
            reportIntervalPowerMin_, reportIntervalPowerMax_, reportDifferencePower_)
    }
    else{
        if (logEnable) log.debug("DISABLE Power Reporting")
        cmds += zigbee.configureReporting(0x0702, 0x0400, DataType.INT24, 5, 65535, 1)
    }

    if (reportEnergy){
        if (logEnable) log.debug("ENABLE Energy Reporting")
        cmds += zigbee.configureReporting(0x0702, 0x0000, DataType.UINT48,
            reportIntervalEnergyMin_, reportIntervalEnergyMax_, reportDifferenceEnergy_)
    }
    else{
        if (logEnable) log.debug("DISABLE Energy Reporting")
        cmds += zigbee.configureReporting(0x0702, 0x0000, DataType.UINT48, 5, 65535, 1)
    }

    if (!isTemperatureSupported()){
        if (logEnable) log.debug("Temperature Reporting is NOT SUPPORTED")
    }
    else if (reportTemperature){
        if (logEnable) log.debug("ENABLE Temperature Reporting")
        cmds += zigbee.configureReporting(0x0002, 0x0000, DataType.INT16,
            reportIntervalTemperatureMin_, reportIntervalTemperatureMax_, reportDifferenceTemperature_)
    }
    else{
        if (logEnable) log.debug("DISABLE Temperature Reporting")
        cmds += zigbee.configureReporting(0x0002, 0x0000, DataType.INT16, 5, 65535, 1)
    }
    if (logEnable) log.debug(cmds)
    return cmds
}

def reset(){
	if (logEnable) log.debug "reset"
    zigbee.writeAttribute(0x0702, 0x0099, DataType.UINT8, 00)
}

def setReporting (attribute=null, value=null){
    int reportIntervalPowerMin_ = (int)reportIntervalPowerMin
    int reportIntervalPowerMax_ = (int)reportIntervalPowerMax
    int reportDifferencePower_ = (int)reportDifferencePower
    int reportIntervalEnergyMin_ = (int)reportIntervalEnergyMin
    int reportIntervalEnergyMax_ = (int)reportIntervalEnergyMax
    int reportDifferenceEnergy_ = (int)reportDifferenceEnergy
    int reportIntervalTemperatureMin_ = (int)reportIntervalTemperatureMin
    int reportIntervalTemperatureMax_ = (int)reportIntervalTemperatureMax
    int reportDifferenceTemperature_ = (int)reportDifferenceTemperature

    if ((attribute&&vaule) == null) log.warn "Invalid input"
    else {
        switch (attribute){
            case "Power":
                if (value == "Enabled"){
                    if (logEnable) log.debug("ENABLE Power Reporting")
                    device.updateSetting("reportPower", true)
                    zigbee.configureReporting(0x0702, 0x0400, DataType.INT24,
                        reportIntervalPowerMin_, reportIntervalPowerMax_, reportDifferencePower_)
                }
                else {
                    if (logEnable) log.debug("DISABLE Power Reporting")
                    device.updateSetting("reportPower", false)
                    zigbee.configureReporting(0x0702, 0x0400, DataType.INT24, 5, 65535, 1)
                }
                break
            case "Energy":
                if (value == "Enabled"){
                    if (logEnable) log.debug("ENABLE Energy Reporting")
                    device.updateSetting("reportEnergy", true)
                    zigbee.configureReporting(0x0702, 0x0000, DataType.UINT48,
                        reportIntervalEnergyMin_, reportIntervalEnergyMax_, reportDifferenceEnergy_)
                }
                else {
                    if (logEnable) log.debug("DISABLE Energy Reporting")
                    device.updateSetting("reportEnergy", false)
                    zigbee.configureReporting(0x0702, 0x0000, DataType.UINT48, 5, 65535, 1)
                }
                break
            case "Temperature":
                if (!isTemperatureSupported()){
                    if (logEnable) log.debug("Temperature Reporting is NOT SUPPORTED")
                }
                else if (value == "Enabled"){
                    if (logEnable) log.debug("ENABLE Temperature Reporting")
                    device.updateSetting("reportTemperature", true)
                    zigbee.configureReporting(0x0002, 0x0000, DataType.INT16,
                        reportIntervalTemperatureMin_, reportIntervalTemperatureMax_, reportDifferenceTemperature_)
                    
                }
                else {
                    if (logEnable) log.debug("DISABLE Temperature Reporting")
                    device.updateSetting("reportTemperature", false)
                    zigbee.configureReporting(0x0002, 0x0000, DataType.INT16, 5, 65535, 1)
                }
                break
            default:
                break
        }
    }
}

def setReportingParams (attribute=null, value=null){
    int reportIntervalPowerMin_ = (int)reportIntervalPowerMin
    int reportIntervalPowerMax_ = (int)reportIntervalPowerMax
    int reportDifferencePower_ = (int)reportDifferencePower
    int reportIntervalEnergyMin_ = (int)reportIntervalEnergyMin
    int reportIntervalEnergyMax_ = (int)reportIntervalEnergyMax
    int reportDifferenceEnergy_ = (int)reportDifferenceEnergy
    int reportIntervalTemperatureMin_ = (int)reportIntervalTemperatureMin
    int reportIntervalTemperatureMax_ = (int)reportIntervalTemperatureMax
    int reportDifferenceTemperature_ = (int)reportDifferenceTemperature
    
    if ((attribute&&vaule) == null) log.warn "Invalid input"
    else{
        int value_ = (int) value
        switch(attribute){
            case "PowerIntervalMin":
                if (logEnable) log.debug("ENABLE Power Reporting")
                device.updateSetting("reportPower", true)
                device.updateSetting("reportIntervalPowerMin", [value: value, type: "number"])
                zigbee.configureReporting(0x0702, 0x0400, DataType.INT24,
                    value_, reportIntervalPowerMax_, reportDifferencePower_)
                break
            case "PowerIntervalMax":
                if (logEnable) log.debug("ENABLE Power Reporting")
                device.updateSetting("reportPower", true)
                device.updateSetting("reportIntervalPowerMax", [value: value, type: "number"])
                zigbee.configureReporting(0x0702, 0x0400, DataType.INT24,
                    reportIntervalPowerMin_, value_, reportDifferencePower_)
                break
            case "PowerDifference":
                if (logEnable) log.debug("ENABLE Power Reporting")
                device.updateSetting("reportPower", true)
                device.updateSetting("reportDifferencePower", [value: value, type: "number"])
                zigbee.configureReporting(0x0702, 0x0400, DataType.INT24,
                    reportIntervalPowerMin_, reportIntervalPowerMax_, value_)
                break
            case "EnergyIntervalMin":
                if (logEnable) log.debug("ENABLE Energy Reporting")
                device.updateSetting("reportEnergy", true)
                device.updateSetting("reportIntervalEnergyMin", [value: value, type: "number"])
                zigbee.configureReporting(0x0702, 0x0000, DataType.UINT48,
                   value_, reportIntervalEnergyMax_, reportDifferenceEnergy_)                
                break
            case "EnergyIntervalMax":
                if (logEnable) log.debug("ENABLE Energy Reporting")
                device.updateSetting("reportEnergy", true)
                device.updateSetting("reportIntervalEnergyMax", [value: value, type: "number"])
                zigbee.configureReporting(0x0702, 0x0000, DataType.UINT48,
                   reportIntervalEnergyMin_, value_, reportDifferenceEnergy_) 
                break
            case "EnergyDifference":
                if (logEnable) log.debug("ENABLE Energy Reporting")
                device.updateSetting("reportEnergy", true)
                device.updateSetting("reportDifferenceEnergy", [value: value, type: "number"])
                zigbee.configureReporting(0x0702, 0x0000, DataType.UINT48,
                   reportIntervalEnergyMin_, reportIntervalEnergyMax_, value_) 
                break
            case "TemperatureIntervalMin":
                if (!isTemperatureSupported()){
                    if (logEnable) log.debug("Temperature Reporting is NOT SUPPORTED")
                }
                else{
                    if (logEnable) log.debug("ENABLE Temperature Reporting")
                    device.updateSetting("reportTemperature", true)
                    device.updateSetting("reportIntervalTemperatureMin", [value: value, type: "number"])
                    zigbee.configureReporting(0x0002, 0x0000, DataType.INT16,
                        value_, reportIntervalTemperatureMax_, reportDifferenceTemperature_)
                }
                break
            case "TemperatureIntervalMax":
                if (!isTemperatureSupported()){
                    if (logEnable) log.debug("Temperature Reporting is NOT SUPPORTED")
                }
                else{
                    if (logEnable) log.debug("ENABLE Temperature Reporting")
                    device.updateSetting("reportTemperature", true)
                    device.updateSetting("reportIntervalTemperatureMax", [value: value, type: "number"])
                    zigbee.configureReporting(0x0002, 0x0000, DataType.INT16,
                        reportIntervalTemperatureMin_, value_, reportDifferenceTemperature_)
                }
                break
            case "TemperatureDifference":
                if (!isTemperatureSupported()){
                    if (logEnable) log.debug("Temperature Reporting is NOT SUPPORTED")
                }
                else{
                    if (logEnable) log.debug("ENABLE Temperature Reporting")
                    device.updateSetting("reportTemperature", true)
                    device.updateSetting("reportDifferenceTemperature", [value: value, type: "number"])
                    zigbee.configureReporting(0x0002, 0x0000, DataType.INT16,
                        reportIntervalTemperatureMin_, reportIntervalTemperatureMax_, value_)
                }
                break
        }
    }
}

private int getPowerDiv() {
    1
}

private int getEnergyDiv() {
    1000
}