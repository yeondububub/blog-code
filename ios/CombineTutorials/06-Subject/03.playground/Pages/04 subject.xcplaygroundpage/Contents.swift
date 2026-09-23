import SwiftUI
import Combine
import PlaygroundSupport

//class ViewModel: ObservableObject {
class ViewModel {
    //@Published var value = 0
    var subject = CurrentValueSubject<Int, Never>(0)
}

struct ContentView: View {
    
    //@State var value = 0
    //@ObservedObject var viewModel = ViewModel()
    var viewModel = ViewModel()
    
    var body: some View {
        VStack {
            Text("count: \(viewModel.subject.value)")
            Button("1증가 버튼") {
                viewModel.subject.send(viewModel.subject.value + 1)
                print(viewModel.subject.value)
            }
        }
    }
}

PlaygroundPage.current.setLiveView(ContentView())
