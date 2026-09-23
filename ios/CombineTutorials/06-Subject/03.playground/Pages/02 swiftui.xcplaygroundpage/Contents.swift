import SwiftUI
import Combine
import PlaygroundSupport

struct ContentView: View {
    
    @State var value = 0
    
    var body: some View {
        VStack {
            Text("count: \(value)")
            Button("1증가 버튼") {
                value += 1
            }
        }
    }
}

PlaygroundPage.current.setLiveView(ContentView())
