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
                    if context.state.progress >= 1.0 || context.state.estimatedDeliveryTime <= Date() {
                        Text("배달 완료")
                            .font(.caption)
                            .bold()
                            .foregroundStyle(.green)
                    } else {
                        Text(timerInterval: Date()...context.state.estimatedDeliveryTime, countsDown: true)
                            .font(.caption)
                            .foregroundStyle(.orange)
                    }
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
        VStack(spacing: 12) {
            // 상단 헤더: 브랜드 및 주문 번호
            HStack(alignment: .center) {
                HStack(spacing: 8) {
                    Image(systemName: "fork.knife.circle.fill")
                        .font(.title3)
                        .foregroundStyle(.orange)
                    Text(context.attributes.restaurantName)
                        .font(.headline)
                        .bold()
                        .foregroundStyle(.white)
                }
                
                Spacer()
                
                Text("주문번호 \(context.attributes.orderNumber)")
                    .font(.caption2)
                    .foregroundStyle(.white.opacity(0.7))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.white.opacity(0.12))
                    .clipShape(Capsule())
            }
            
            // 중단 정보: 배달 상태 문구 및 예상 도착 시간
            HStack(alignment: .lastTextBaseline) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("배달 현황")
                        .font(.caption2)
                        .foregroundStyle(.white.opacity(0.5))
                    Text(context.state.statusText)
                        .font(.title3)
                        .bold()
                        .foregroundStyle(context.state.progress >= 1.0 ? .green : .orange)
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 4) {
                    Text(context.state.progress >= 1.0 ? "도착 완료" : "도착 예정")
                        .font(.caption2)
                        .foregroundStyle(.white.opacity(0.5))
                    Text(context.state.estimatedDeliveryTime, style: .time)
                        .font(.title3)
                        .bold()
                        .foregroundStyle(context.state.progress >= 1.0 ? .green : .white)
                }
            }
            
            // 하단 게이지 바 및 단계별 라벨
            VStack(spacing: 6) {
                ProgressView(value: context.state.progress)
                    .progressViewStyle(LinearProgressViewStyle(tint: .orange))
                    .scaleEffect(x: 1, y: 1.8, anchor: .center)
                    .clipShape(Capsule())
                
                HStack {
                    Text("접수")
                        .foregroundStyle(context.state.progress >= 0.1 ? .orange : .white.opacity(0.3))
                    Spacer()
                    Text("조리")
                        .foregroundStyle(context.state.progress >= 0.3 ? .orange : .white.opacity(0.3))
                    Spacer()
                    Text("배달")
                        .foregroundStyle(context.state.progress >= 0.7 ? .orange : .white.opacity(0.3))
                    Spacer()
                    Text("완료")
                        .foregroundStyle(context.state.progress >= 1.0 ? .orange : .white.opacity(0.3))
                }
                .font(.system(size: 11, weight: .semibold))
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
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
