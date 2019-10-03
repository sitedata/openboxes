/**
* Copyright (c) 2012 Partners In Health.  All rights reserved.
* The use and distribution terms for this software are covered by the
* Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
* which can be found in the file epl-v10.html at the root of this distribution.
* By using this software in any fashion, you are agreeing to be bound by
* the terms of this license.
* You must not remove this notice, or any other, from this software.
**/ 
package org.pih.warehouse.report

import org.grails.plugins.web.taglib.RenderTagLib
import grails.web.context.ServletContextHolder
import org.pih.warehouse.core.Location
import org.pih.warehouse.core.RoleType
import org.pih.warehouse.core.User
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.WebApplicationContextUtils

class NotificationService {

    def dataService
    def userService
    def mailService

    boolean transactional = false

    def getExpiryAlertsByLocation(Location location, Integer daysUntilExpiry = 0) {
        String query = """
            select * 
            from product_inventory_expiry_view 
            where days_until_expiry <= ${daysUntilExpiry} 
            and location_id = '${location.id}'
            and quantity_on_hand > 0
            order by days_until_expiry asc
            """
        return dataService.executeQuery(query)
    }

    def getStockAlertsByLocation(Location location) {
        String query = """
            select * 
            from product_inventory_compare_view 
            where location_id = '${location.id}'
            order by product_name asc
            """
        return dataService.executeQuery(query)
    }

    def sendExpiryAlerts(Location location, Integer daysUntilExpiry = 60, List<RoleType> roleTypes, Boolean skipOnEmpty) {
        def subject = "Expiry Alerts - ${location.name}"
        def expiryAlerts = getExpiryAlertsByLocation(location, daysUntilExpiry)
        if (expiryAlerts.isEmpty() && skipOnEmpty) {
            log.info "Skipped ${subject} email for location ${location} because there are no alerts"
            return
        }
        def subscribers = userService.findUsersByRoleTypes(location, roleTypes)
        def csv = dataService.generateCsv(expiryAlerts)
        def expired = expiryAlerts.findAll { it.days_until_expiry <= 0 }
        def expiring = expiryAlerts.findAll { it.days_until_expiry > 0 }
        def model = [location: location, expiring: expiring, expired: expired, daysUntilExpiry: daysUntilExpiry]
        log.info "Sending ${expiryAlerts.size()} ${subject} alerts and ${subscribers.size()} subscribers for location ${location}"
        sendAlerts(subject, "/email/expiryAlerts", model, subscribers, csv)
    }

    def sendStockAlerts(Location location, String status, List<RoleType> roleTypes, Boolean skipOnEmpty) {
        def subject = "Stock Alerts - Status ${status} - ${location.name}"
        def stockAlerts = getStockAlertsByLocation(location)
        def products = stockAlerts.findAll { it.status == status }
        if (products.isEmpty() && skipOnEmpty) {
            log.info "Skipped ${subject} email for location ${location} because there are no alerts"
            return
        }
        def subscribers = userService.findUsersByRoleTypes(location, roleTypes)
        def model = [location: location, status: status, products: products]
        def csv = dataService.generateCsv(products)
        log.info "Sending ${products.size()} ${subject} alerts and ${subscribers.size()} subscribers for location ${location} "
        sendAlerts(subject, "/email/stockAlerts", model, subscribers, csv)
    }

    def sendAlerts(String subject, String template, Map model, List<User> subscribers, String csv) {

        Collection toList = subscribers.collect { it.email }.findAll{ it != null }.toArray()
        if (toList.isEmpty()) {
            log.info("Skipped ${subject} email because there are no subscribers")
            return
        }

        // FIXME Need to fix this when we migrate to grails 3
        // Hack to ensure that the GSP template engine has access to a request.
        def webRequest = RequestContextHolder.getRequestAttributes()
        if(!webRequest) {
            def servletContext = ServletContextHolder.getServletContext()
            def applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext)
            grails.util.GrailsWebUtil.bindMockWebRequest(applicationContext)
        }

        def renderTagLib = new RenderTagLib()
        String body = renderTagLib.render(template: template, model: model)

        // Send email with attachment (if csv exists)
        if (csv) {
            mailService.sendHtmlMailWithAttachment(toList, [], subject, body, csv.bytes, "${subject}.csv", "text/csv")
        }
        else {
            mailService.sendHtmlMail(subject, body, toList)
        }

    }


}
