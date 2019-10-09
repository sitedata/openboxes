/**
 * Copyright (c) 2012 Partners In Health.  All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 **/
package org.pih.warehouse.inventory

import org.pih.warehouse.auth.AuthService
import org.pih.warehouse.core.Constants
import org.pih.warehouse.jobs.RefreshInventorySnapshotJob
import org.pih.warehouse.requisition.Requisition
import org.pih.warehouse.shipping.Shipment
import org.pih.warehouse.core.Location
import org.pih.warehouse.core.User
import org.pih.warehouse.order.Order
import org.pih.warehouse.receiving.Receipt

/**
 *  Represents a unit of work completed within a single warehouse.  A
 *  transaction can be incoming/outgoing and must have a source and
 *  destination.
 *
 *  All warehouse events (like shipments, deliveries) are managed through
 *  transactions.  All warehouse events have two transactions (an incoming
 *  transaction for the destination warehouse and an outgoing transaction
 *  for the source warehouse.
 *
 *  A transaction consists of multiple transaction items (which are
 *  just individual products, that are added to or subtracted from
 *  the inventory).  For every warehouse event, a warehouse
 *  will be represented in two transactions (incoming and outgoing).
 *
 */
class Transaction implements Comparable, Serializable {

    def beforeInsert = {
        def currentUser = AuthService.currentUser.get()
        if (currentUser) {
            createdBy = currentUser
            updatedBy = currentUser
        }
    }
    def beforeUpdate = {
        def currentUser = AuthService.currentUser.get()
        if (currentUser) {
            updatedBy = currentUser
        }
    }

    def triggerRefreshInventorySnapshotJob = {
        RefreshInventorySnapshotJob.triggerNow([startDate: transactionDate, location: inventory?.warehouse?.id])
    }


    // ID won't be available until after the record is inserted
    def afterInsert = triggerRefreshInventorySnapshotJob

    def afterUpdate = triggerRefreshInventorySnapshotJob

    // This probably needs to be "before" since the transaction will not be around after
    def afterDelete = triggerRefreshInventorySnapshotJob

    String id
    TransactionType transactionType
    // Detailed transaction type (e.g. Order, Transfer, Stock Count)
    Date transactionDate                // Date entered into the warehouse
    String transactionNumber
    Location source
    Location destination
    String comment

    Shipment outgoingShipment
    // Outgoing shipment associated with a transfer out transasction
    Shipment incomingShipment
    // Incoming shipment associated with a transfer in transasction
    Requisition requisition                // associated requisition
    Receipt receipt
    Order order

    // Auditing fields
    Boolean confirmed = Boolean.FALSE    // Transactions need to be confirmed by a supervisor
    User confirmedBy
    Date dateConfirmed
    List transactionEntries

    User createdBy
    User updatedBy
    Date dateCreated
    Date lastUpdated

    Inventory inventory

    // Association mapping
    static hasMany = [transactionEntries: TransactionEntry]

    static mapping = {
        id generator: 'uuid'
        transactionEntries cascade: "all-delete-orphan"
    }

    // Transient attributs
    static transients = ['localTransfer']


    static namedQueries = {
        minTransactionDate {
            projections {
                min 'transactionDate'
            }
            uniqueResult = true
        }

        maxTransactionDate {
            projections {
                max 'transactionDate'
            }
            uniqueResult = true
        }
    }

    // Constraints 
    static constraints = {
        transactionType(nullable: false)
        transactionNumber(nullable: true, unique: true)
        createdBy(nullable: true)
        updatedBy(nullable: true)
        outgoingShipment(nullable: true)
        incomingShipment(nullable: true)
        requisition(nullable: true)
        receipt(nullable: true)
        order(nullable: true)
        confirmed(nullable: true)
        confirmedBy(nullable: true)
        dateConfirmed(nullable: true)
        comment(nullable: true)
        transactionDate(nullable: false,
                validator: { value -> value <= new Date() })
        // transaction date cannot be in the future

        source(nullable: true,
                validator: { value, obj ->
                    if (value && obj.destination) {
                        return false
                    }   // transaction cannot have both a source and a destination
                    if (obj.transactionType?.id == Constants.TRANSFER_IN_TRANSACTION_TYPE_ID && !value) {
                        return false
                    } // transfer in transaction must have source
                    return true
                })

        destination(nullable: true,
                validator: { value, obj ->
                    if (value && obj.source) {
                        return false
                    }  // transaction cannot have both a source and a destination
                    if (obj.transactionType?.id == Constants.TRANSFER_OUT_TRANSACTION_TYPE_ID && !value) {
                        return false
                    } // transfer out transaction must have destination
                    return true
                })
    }


    LocalTransfer getLocalTransfer() {
        return LocalTransfer.findByDestinationTransactionOrSourceTransaction(this, this)
    }


    /**
     * Sort by transaction date, and then by date created
     * (Note that sorting of transaction entries, and therefore the whole inventory process, relies on this
     *  so don't make changes lightly!)
     */
    int compareTo(obj) {
        def compare = transactionDate <=> obj.transactionDate
        if (compare == 0) {
            compare = dateCreated <=> obj.dateCreated
        }
        return compare
    }
}
