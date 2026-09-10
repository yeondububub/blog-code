//
//  DeliveryActivityWidgetLiveActivity.swift
//  DeliveryActivityWidget
//
//  Created by Junmo Sung on 9/10/26.
//

import ActivityKit
import WidgetKit
import SwiftUI

struct DeliveryActivityWidgetAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        // Dynamic stateful properties about your activity go here!
        var emoji: String
    }

    // Fixed non-changing properties about your activity go here!
    var name: String
}

struct DeliveryActivityWidgetLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: DeliveryActivityWidgetAttributes.self) { context in
            // Lock screen/banner UI goes here
            VStack {
                Text("Hello \(context.state.emoji)")
            }
            .activityBackgroundTint(Color.cyan)
            .activitySystemActionForegroundColor(Color.black)

        } dynamicIsland: { context in
            DynamicIsland {
                // Expanded UI goes here.  Compose the expanded UI through
                // various regions, like leading/trailing/center/bottom
                DynamicIslandExpandedRegion(.leading) {
                    Text("Leading")
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text("Trailing")
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text("Bottom \(context.state.emoji)")
                    // more content
                }
            } compactLeading: {
                Text("L")
            } compactTrailing: {
                Text("T \(context.state.emoji)")
            } minimal: {
                Text(context.state.emoji)
            }
            .widgetURL(URL(string: "http://www.apple.com"))
            .keylineTint(Color.red)
        }
    }
}

extension DeliveryActivityWidgetAttributes {
    fileprivate static var preview: DeliveryActivityWidgetAttributes {
        DeliveryActivityWidgetAttributes(name: "World")
    }
}

extension DeliveryActivityWidgetAttributes.ContentState {
    fileprivate static var smiley: DeliveryActivityWidgetAttributes.ContentState {
        DeliveryActivityWidgetAttributes.ContentState(emoji: "😀")
     }
     
     fileprivate static var starEyes: DeliveryActivityWidgetAttributes.ContentState {
         DeliveryActivityWidgetAttributes.ContentState(emoji: "🤩")
     }
}

#Preview("Notification", as: .content, using: DeliveryActivityWidgetAttributes.preview) {
   DeliveryActivityWidgetLiveActivity()
} contentStates: {
    DeliveryActivityWidgetAttributes.ContentState.smiley
    DeliveryActivityWidgetAttributes.ContentState.starEyes
}
