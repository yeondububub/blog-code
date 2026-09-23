import SwiftUI
import Combine
import PlaygroundSupport

class ViewModel: ObservableObject {
    @Published var value = 0
}

struct ContentView: View {
    
    //@State var value = 0
    @ObservedObject var viewModel = ViewModel()
    
    var body: some View {
        VStack {
            Text("count: \(viewModel.value)")
            Button("1증가 버튼") {
                viewModel.value += 1
            }
        }
    }
}

PlaygroundPage.current.setLiveView(ContentView())
