/**
 *  Copyright 2019 SmartThings
 *  Copyright 2020-2022 Donghwan Suh
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
        input name: "reportTemperature", type: "bool", title: "온도 리포팅 (Enable temperature reporting)", description: "", required: true, defaultValue: true
        input name: "reportIntervalTemperatureMin", type: "number", title: "온도 리포팅 최소 시간 간격 (Minimum interval of temperature reporting)", range: "1..65534", required: true, defaultValue: 1
        input name: "reportIntervalTemperatureMax", type: "number", title: "온도 리포팅 최대 시간 간격 (Maximum interval of temperature reporting)", range: "1..65534", required: true, defaultValue: 600
        input name: "reportDifferenceTemperature", type: "number", title: "온도 리포팅을 위한 최소 변동량 (Difference required to report temperature)", range: "1..65534", required: true, defaultValue: 1
        input name: "logEnable", type: "bool", title: "디버그 로그 (Enable debug logging)", defaultValue: false
        input name: "descriptionEnable", type: "bool", title: "드라이버 설명서 표시 (Show the manual of the driver)", defaultValue: true
    }
}

def installed() {
    state.clear()
    state.DescriptionKR = "(1) 내부 온도 측정은 PM-B540-ZB 모델만 지원합니다. (2) PM-B540-ZB 이외의 모델은 리포팅 설정이 적용되지 않을 수 있습니다. (3) 플러그 자체의 전원이 재인가된 경우나 'reset' 명령을 사용한 경우 리포팅 설정이 초기화 되므로 'congfigure' 명령을 호출해야 리포팅 설정이 적용됩니다."
    state.DescriptionEN = "(1) Temperature reporting is only supported on PM-B540-ZB. (2) Reporting configuration might not work on models other than PM-B540-ZB. (3) Since the reporting configuration of the outlet resets when the outlet itself was power-cycled or the 'reset' command was called, calling 'configure' is required to set the reporting settings for those situations."

    configure()
}

def updated() {
    state.clear()
    if (descriptionEnable) { 
        state.DescriptionKR = "(1) 내부 온도 측정은 PM-B540-ZB 모델만 지원합니다. (2) PM-B540-ZB 이외의 모델은 리포팅 설정이 적용되지 않을 수 있습니다. (3) 리포팅 설정 저장 후 'configure' 명령을 호출해야 새로운 설정이 적용됩니다. (4) 플러그 자체의 전원이 재인가된 경우나 'reset' 명령을 사용한 경우 리포팅 설정이 초기화 되므로 'congfigure' 명령을 호출해야 리포팅 설정이 적용됩니다."
        state.DescriptionEN = "(1) Temperature reporting is only supported on PM-B540-ZB. (2) Reporting configuration might not work on models other than PM-B540-ZB. (3) After saving reporting configuration 'configure' should be called to apply the changes. (4) Since the reporting configuration of the outlet resets when the outlet itself was power-cycled or the 'reset' command was called, calling 'configure' is required to set the reporting settings for those situations."
    }
    configure()
    //log.info "updated. Please re-configure the device to apply changes."
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
    if (device.getDataValue("model") == "PM-B540-ZB"){
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
        cmds += zigbee.configureReporting(0x0702, 0x0400, DataType.INT24
        , reportIntervalPowerMin_, reportIntervalPowerMax_, reportDifferencePower_)
    }
    else{
        if (logEnable) log.debug("DISABLE Power Reporting")
        cmds += zigbee.configureReporting(0x0702, 0x0400, DataType.INT24, 5, 65535, 1)
    }

    if (reportEnergy){
        if (logEnable) log.debug("ENABLE Energy Reporting")
        cmds += zigbee.configureReporting(0x0702, 0x0000, DataType.UINT48
        , reportIntervalEnergyMin_, reportIntervalEnergyMax_, reportDifferenceEnergy_)
    }
    else{
        if (logEnable) log.debug("DISABLE Energy Reporting")
        cmds += zigbee.configureReporting(0x0702, 0x0000, DataType.UINT48, 5, 65535, 1)
    }

    if ((device.getDataValue("model") != "PM-B540-ZB") && reportTemperature){
        if (logEnable) log.debug("Temperature Reporting is NOT SUPPORTED")
    }
    else if (reportTemperature){
        if (logEnable) log.debug("ENABLE Temperature Reporting")
        cmds += zigbee.configureReporting(0x0002, 0x0000, DataType.INT16
        , reportIntervalTemperatureMin_, reportIntervalTemperatureMax_, reportDifferenceTemperature_)
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

private int getPowerDiv() {
    1
}

private int getEnergyDiv() {
    1000
}
