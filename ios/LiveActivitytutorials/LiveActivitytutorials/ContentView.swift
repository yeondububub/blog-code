//
//  ContentView.swift
//  LiveActivityTutorials
//
//  Created by Junmo Sung on 9/9/26.
//

import SwiftUI

struct ContentView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                VStack(spacing: 8) {
                    Text("배달 라이브 액티비티 테스트")
                        .font(.title2)
                        .bold()
                    Text("버튼을 누른 후 홈 화면으로 나가거나 잠금 화면, Dynamic Island를 확인해 보세요.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }
                .padding(.top, 40)
                
                Spacer()
                
                // 1. 배달 시작
                Button {
                    DeliveryActivityManager.shard.startDeliveryActivity(
                        orderNumver: "ORD-2026-001",
                        restaurantName: "BHC 치킨"
                    )
                } label: {
                    Label("1. 배달 시작 (주문 접수)", systemImage: "play.circle.fill")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                
                // 2. 조리 중 갱신
                Button {
                    DeliveryActivityManager.shard.updateDeliveryActivity(
                        statusText: "조리 중",
                        progress: 0.3,
                        estimatedTime: Date().addingTimeInterval(30 * 60)
                    )
                } label: {
                    Label("2. 상태 갱신 (조리 중 30%)", systemImage: "flame.fill")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.orange)
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                
                // 3. 배달 중 갱신
                Button {
                    DeliveryActivityManager.shard.updateDeliveryActivity(
                        statusText: "배달 중",
                        progress: 0.7,
                        estimatedTime: Date().addingTimeInterval(10 * 60)
                    )
                } label: {
                    Label("3. 상태 갱신 (배달 중 70%)", systemImage: "bicycle")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                
                // 4. 배달 완료 및 종료
                Button {
                    DeliveryActivityManager.shard.endDeliveryActivity()
                } label: {
                    Label("4. 배달 완료 및 종료", systemImage: "stop.circle.fill")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.red)
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                
                Spacer()
            }
            .padding(.horizontal, 24)
            .navigationTitle("Live Activity")
        }
    }
}

#Preview {
    ContentView()
}
