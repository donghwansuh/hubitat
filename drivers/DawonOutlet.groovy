/**
 *  Copyright 2019 SmartThings
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
 *  2021/01/23 v0.2 removed unused codes.(unused zigbee clusters and checkInterval)
 *  2020/11/24 v0.1 initial version
 *
 *  Modified by DongHwan Suh  
 *  Based on following codes
 *  SmartThings https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/devicetypes/smartthings/zigbee-metering-plug.src/zigbee-metering-plug.groovy
 *  Dawon DNS https://pmshop.co.kr/board/free/read.html?no=5552&board_no=2
 *
 */
import hubitat.zigbee.zcl.DataType

metadata {
    definition (name: "Dawon Outlet", namespace: "donghwansuh", author: "SmartThings/DongHwan Suh") {
        capability "EnergyMeter"
        capability "PowerMeter"
        capability "Actuator"
        capability "Switch"
        capability "Refresh"
        capability "HealthCheck"
        capability "Sensor"
        capability "Configuration"

        command "reset"

        fingerprint endpointId: "01", profileId: "0104", inClusters: "0000, 0003, 0004, 0006, 0019, 0702, 0B04", outClusters: "0000, 0003, 0004, 0006, 0019, 0702, 0B04", manufacturer: "DAWON_DNS", model: "PM-B430-ZB", deviceJoinName: "Dawon Outlet" // DAWON DNS Smart Plug PM-B430-ZB (10A), raw description: 01 0104 0051 01 07 0000, 0004, 0003, 0006, 0019, 0702, 0B04 07 0000, 0004, 0003, 0006, 0019, 0702, 0B04
        fingerprint endpointId: "01", profileId: "0104", inClusters: "0000, 0002, 0003, 0004, 0006, 0019, 0702, 0B04, 0009", outClusters: "0000, 0002, 0003, 0004, 0006, 0019, 0702, 0B04, 0009", manufacturer: "DAWON_DNS", model: "PM-B530-ZB", deviceJoinName: "Dawon Outlet" // DAWON DNS Smart Plug PM-B530-ZB (16A), raw description: 01 0104 0051 01 07 0000, 0004, 0003, 0006, 0019, 0702, 0B04 07 0000, 0004, 0003, 0006, 0019, 0702, 0B04
        fingerprint manufacturer: "DAWON_DNS", model: "PM-C140-ZB", deviceJoinName: "Dawon Outlet" // DAWON DNS In-Wall Outlet PM-C140-ZB, raw description: 01 0104 0051 01 0A 0000 0002 0003 0004 0006 0019 0702 0B04 0008 0009 0A 0000 0002 0003 0004 0006 0019 0702 0B04 0008 0009
        fingerprint profileId: "0104", inClusters: "0000, 0002, 0003, 0006, 0702, 0B04", outClusters: "0003, 0019", manufacturer: "DAWON_DNS", model: "PM-B540-ZB",  deviceJoinName: "Dawon Outlet" // DAWON DNS Smart Plug
        fingerprint profileId: "0104", inClusters: "0000, 0002, 0003, 0006, 0702, 0B04", outClusters: "0003, 0019", manufacturer: "DAWON_DNS", model: "ST-B550-ZB",  deviceJoinName: "Dawon Outlet" // DAWON DNS Smart Plug
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0006, 0702, 0B04", outClusters: "0003, 0019", manufacturer: "DAWON_DNS", model: "PM-C150-ZB",  deviceJoinName: "Dawon Outlet" // DAWON DNS In-Wall Outlet
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0006, 0702, 0B04", outClusters: "0003, 0019", manufacturer: "DAWON_DNS", model: "PM-C250-ZB",  deviceJoinName: "Dawon Outlet" // DAWON DNS In-Wall Outlet
	}

}

def getATTRIBUTE_READING_INFO_SET() { 0x0000 }
def getATTRIBUTE_HISTORICAL_CONSUMPTION() { 0x0400 }
def getSIMPLE_METERING_CLUSTER() { 0x0702 }

def parse(String description) {
    log.debug "description is $description"
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
        log.info "event outer:$event"
        sendEvent(event)
    } else {
        List result = []
        log.debug "Desc Map: $descMap"

        List attrData = [[clusterInt: descMap.clusterInt ,attrInt: descMap.attrInt, value: descMap.value]]
        descMap.additionalAttrs.each {
            attrData << [clusterInt: descMap.clusterInt, attrInt: it.attrInt, value: it.value]
        }

        attrData.each {
                def map = [:]
                if (it.value && it.clusterInt == SIMPLE_METERING_CLUSTER && it.attrInt == ATTRIBUTE_HISTORICAL_CONSUMPTION) {
                        log.debug "power"
                        map.name = "power"
                        map.value = zigbee.convertHexToInt(it.value)/getPowerDiv()
                        map.unit = "W"
                }
                else if (it.value && it.clusterInt == SIMPLE_METERING_CLUSTER && it.attrInt == ATTRIBUTE_READING_INFO_SET) {
                        log.debug "energy"
                        map.name = "energy"
                        map.value = zigbee.convertHexToInt(it.value)/getEnergyDiv()
                        map.unit = "kWh"
                }

                if (map) {
                        result << createEvent(map)
                }
                log.debug "Parse returned $map"
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
    log.debug "refresh"
    zigbee.onOffRefresh() +
    zigbee.readAttribute(SIMPLE_METERING_CLUSTER, ATTRIBUTE_READING_INFO_SET)
}

def configure() {
    log.debug "Configuring Reporting"
    def cmds = refresh() +
    	   zigbee.onOffConfig() +
           zigbee.configureReporting(SIMPLE_METERING_CLUSTER, ATTRIBUTE_READING_INFO_SET, DataType.UINT48, 1, 600, 1) +
           zigbee.configureReporting(SIMPLE_METERING_CLUSTER, ATTRIBUTE_HISTORICAL_CONSUMPTION, DataType.INT24, 1, 600, 1)
    return cmds
}

def reset(){
	log.debug "reset"
    zigbee.writeAttribute(SIMPLE_METERING_CLUSTER, 0x0099, DataType.UINT8, 00)
}

private int getPowerDiv() {
    1
}

private int getEnergyDiv() {
    1000
}