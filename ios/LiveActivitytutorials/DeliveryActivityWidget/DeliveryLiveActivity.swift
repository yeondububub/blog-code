//
//  DeliveryActivityWidgetLiveActivity.swift
//  DeliveryActivityWidget
//
//  Created by Junmo Sung on 9/10/26.
//

import ActivityKit
import WidgetKit
import SwiftUI

struct DeliveryLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: DeliveryActivityAttributes.self) { context in
            lockScreenLiveActivityView(context: context)
                .activityBackgroundTint(Color.black.opacity(0.8))
                .activitySystemActionForegroundColor(Color.white)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label(context.attributes.restaurantName, systemImage: "bag.fill")
                        .font(.caption)
                        .foregroundStyle(.primary)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(timerInterval: Date()...context.state.estimatedDeliveryTime, countsDown: true)
                        .font(.caption)
                        .foregroundStyle(.orange)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(context.state.statusText)
                            .font(.headline)
                        ProgressView(value: context.state.progress)
                            .tint(.orange)
                    }
                    .padding(.horizontal)
                }
            } compactLeading: {
                Image(systemName: "bag.fill")
                    .foregroundStyle(.orange)
            } compactTrailing: {
                Text(context.state.statusText)
                    .font(.caption2)
                    .bold()
            } minimal: {
                Image(systemName: "bag.fill")
                    .foregroundStyle(.orange)
            }
            .keylineTint(.orange)
        }

    }
}

struct lockScreenLiveActivityView: View {
    let context: ActivityViewContext<DeliveryActivityAttributes>
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(context.attributes.restaurantName)
                    .font(.headline)
                    .foregroundStyle(.white)
                Spacer()
                Text("주문번호: \(context.attributes.orderNumber)")
                    .font(.caption)
                    .foregroundStyle(.gray)
            }
            
            HStack {
                Text(context.state.statusText)
                    .font(.subheadline)
                    .foregroundStyle(.orange)
                Spacer()
                Text(context.state.estimatedDeliveryTime, style: .time)
                    .font(.subheadline)
                    .bold()
                    .foregroundStyle(.white)
            }
            
            ProgressView(value: context.state.progress)
                .progressViewStyle(LinearProgressViewStyle(tint: .orange))
        }
    }
}

extension DeliveryActivityAttributes {
    fileprivate static var preview: DeliveryActivityAttributes {
        DeliveryActivityAttributes(orderNumber: "ORD-2026-001", restaurantName: "BHC")
    }
}

extension DeliveryActivityAttributes.ContentState {
    fileprivate static var cooking: DeliveryActivityAttributes.ContentState {
        DeliveryActivityAttributes.ContentState(
            statusText: "조리 중", estimatedDeliveryTime: Date().addingTimeInterval(30 * 60), progress: 0.3)
    }
    
    fileprivate static var delivering: DeliveryActivityAttributes.ContentState {
        DeliveryActivityAttributes.ContentState(
            statusText: "배달 중", estimatedDeliveryTime: Date().addingTimeInterval(10 * 60), progress: 0.7)
    }
}

#Preview("Delivery Live Activity", as: .content, using: DeliveryActivityAttributes.preview) {
    DeliveryLiveActivity()
} contentStates: {
    DeliveryActivityAttributes.ContentState.cooking
    DeliveryActivityAttributes.ContentState.delivering
}
