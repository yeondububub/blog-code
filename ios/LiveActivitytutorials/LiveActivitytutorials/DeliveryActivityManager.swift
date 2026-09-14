//
//  DeliveryActivityManager.swift
//  LiveActivityTutorials
//
//  Created by Junmo Sung on 9/12/26.
//

import Foundation
import ActivityKit

final class DeliveryActivityManager {
    static let shard = DeliveryActivityManager()
    private var currentActivity: Activity<DeliveryActivityAttributes>?
    
    private init() {}
    
    // 1. 엑티비티 시작 요청
    func startDeliveryActivity(orderNumver: String, restaurantName: String) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else {
            print("라이브 엑티비티가 비활성화 되어 있습니다.")
            return
        }
        
        let attributes = DeliveryActivityAttributes(
            orderNumber: orderNumver,
            restaurantName: restaurantName
        )
        
        let initialContentState = DeliveryActivityAttributes.ContentState(statusText: "주문 접수 완료", estimatedDeliveryTime: Date().addingTimeInterval(30 * 60), progress: 0.1)
        
        let activityContent = ActivityContent(state: initialContentState, staleDate: nil)
        
        do {
            let activity = try Activity<DeliveryActivityAttributes>.request(
                attributes: attributes,
                content: activityContent,
                pushType: nil
            )
            self.currentActivity = activity
            print("엑티비티 등록 성공! ID: \(activity.id) ")
            
            Task {
                for await pushToken in activity.pushTokenUpdates {
                    let tokenString = pushToken.map {
                        String(format: "%02x", $0)
                    }.joined()
                    print("라이브 엑티비티 전용 푸시 토큰: \(tokenString)")
                }
            }
        } catch {
            print("엑티비티 시작 실패: \(error.localizedDescription)")
        }
    }
    
    // 2. 엑티비티 상태 경신
    func updateDeliveryActivity(statusText: String, progress: Double, estimatedTime: Date) {
        guard let activity = currentActivity else { return }
        
        let updatedContentState = DeliveryActivityAttributes.ContentState(statusText: statusText, estimatedDeliveryTime: estimatedTime, progress: progress)
        
        let activityContent = ActivityContent(state: updatedContentState, staleDate: nil)
        
        Task {
            await activity.update(activityContent)
            print("엑티비티 갱신 완료: \(statusText)")
        }
    }
    
    // 3. 액티비티 종료 (기본 1분간 완료 상태 유지 후 자동 소멸)
    func endDeliveryActivity(dismissalPolicy: ActivityUIDismissalPolicy = .after(Date().addingTimeInterval(60))) {
        guard let activity = currentActivity else { return }
        
        let finalContentState = DeliveryActivityAttributes.ContentState(
            statusText: "배달 완료",
            estimatedDeliveryTime: Date(),
            progress: 1.0
        )
        
        let activityContent = ActivityContent(state: finalContentState, staleDate: nil)
        
        Task {
            await activity.end(activityContent, dismissalPolicy: dismissalPolicy)
            self.currentActivity = nil
            print("액티비티 종료 완료 (상태 유지 후 자동 제거)")
        }
    }
}
