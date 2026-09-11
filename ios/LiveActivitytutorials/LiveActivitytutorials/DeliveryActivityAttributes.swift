//
//  DeliveryActivityAttributes.swift
//  LiveActivityTutorials
//
//  Created by Junmo Sung on 9/11/26.
//

import Foundation
import ActivityKit

struct DeliveryActivityAttributes: ActivityAttributes {
    
    public struct ContentState: Codable, Hashable {
        var statusText: String
        var estimataedDeliveryTime: Date
        var progress: Double
    }
    
    var orderNumber: String
    var restaurantName: String
}
