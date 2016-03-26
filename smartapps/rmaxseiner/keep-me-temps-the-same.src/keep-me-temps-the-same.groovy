definition(
    name: "Keep Me Temps the Same",
    namespace: "rmaxseiner",
    author: "RonMaxseiner",
    description: "This application enables you to pick an alternative temperature sensor in a separate space from the thermostat. The application controls heating and cooling based on the average temperature and system fan settings based on the difference of the two temperatures.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
)

preferences() {
	section("Choose remote thermostat... ") {
		input "thermostat", "capability.thermostat"
	}
	section("Heat setting..." ) {
		input "heatingSetpoint", "decimal", title: "Degrees"
	}
	section("Air conditioning setting...") {
		input "coolingSetpoint", "decimal", title: "Degrees"
	}
	section("Choose Remote Sensor that will be included in Thermostat Control") {
		input "sensor", "capability.temperatureMeasurement", title: "Temp Sensors", required: false
	}
    section("Tollerable Temperature Difference") {
    	input "differenceThresh", "decimal", title: "Tempurature Difference"
    }
}

def installed()
{
	log.debug "enter installed, state: $state"
	subscribeToEvents()
}

def updated()
{
	log.debug "enter updated, state: $state"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents()
{
	subscribe(location, changedLocationMode)
	if (sensor) {
		subscribe(sensor, "temperature", temperatureHandler)
		subscribe(thermostat, "temperature", temperatureHandler)
		subscribe(thermostat, "thermostatMode", temperatureHandler)
	}
	evaluate()
}

def changedLocationMode(evt)
{
	log.debug "changedLocationMode mode: $evt.value, heat: $heat, cool: $cool"
	evaluate()
}

def temperatureHandler(evt)
{
	evaluate()
}

private evaluate()
{
	if (sensor) {
		def threshold = 1.0
		def thermMode = thermostat.currentThermostatMode
		def thermTemp = thermostat.currentTemperature
		def remoteTemp = sensor.currentTemperature
        def averageTemp = (remoteTemp + thermTemp)/2
        def differenceTemp = (remoteTemp - thermTemp)
        differenceTemp = differenceTemp.abs()
        def systemActive = False
		log.trace("evaluate:, mode: $thermMode -- temp: $thermTemp, heat: $thermostat.currentHeatingSetpoint, cool: $thermostat.currentCoolingSetpoint -- "  +
			"sensor: $remoteTemp, heat: $heatingSetpoint, cool: $coolingSetpoint")
		if (thermMode in ["cool","auto"]) {
			// air conditioner
			if (averageTemp - coolingSetpoint >= threshold) {
				thermostat.setCoolingSetpoint(thermTemp - 2)
				log.debug "thermostat.setCoolingSetpoint(${thermTemp - 2}), ON"
                systemActive = True
			}
			else if (coolingSetpoint - averageTemp >= threshold && thermTemp - thermostat.currentCoolingSetpoint >= threshold) {
				thermostat.setCoolingSetpoint(thermTemp + 2)
				log.debug "thermostat.setCoolingSetpoint(${thermTemp + 2}), OFF"
                systemActive = False
			}
		}
		if (thermMode in ["heat","emergency heat","auto"]) {
			// heater
			if (heatingSetpoint - averageTemp >= threshold) {
				thermostat.setHeatingSetpoint(thermTemp + 2)
				log.debug "thermostat.setHeatingSetpoint(${thermTemp + 2}), ON"
                systemActive = True
			}
			else if (averageTemp - heatingSetpoint >= threshold && thermostat.currentHeatingSetpoint - thermTemp >= threshold) {
				thermostat.setHeatingSetpoint(thermTemp - 2)
				log.debug "thermostat.setHeatingSetpoint(${thermTemp - 2}), OFF"
                systemActive = False
			}
		}
        if (!systemActive) {
        	def thermFanMode = thermostat.currentState("thermostatFanMode")
        	if ((differenceTemp > differenceThresh) && (thermFanMode ="auto")) {
            	thermostat.fanOn()
                log.debug "Temp difference: $differnceTemp FanMode: On"         
            } 
            if ((differenceTemp < differenceThresh) && (thermFanMode ="on")) {
            	thermostat.fanAuto()
                log.debug "Temp difference: $differnceTemp FanMode: Auto"                 
            }
            
        }
	}
	else {
		thermostat.setHeatingSetpoint(heatingSetpoint)
		thermostat.setCoolingSetpoint(coolingSetpoint)
		thermostat.poll()
	}
}

// for backward compatibility with existing subscriptions
def coolingSetpointHandler(evt) {
	log.debug "coolingSetpointHandler()"
}
def heatingSetpointHandler (evt) {
	log.debug "heatingSetpointHandler ()"
}