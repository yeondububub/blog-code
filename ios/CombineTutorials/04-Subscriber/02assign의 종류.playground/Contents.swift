import SwiftUI
import Combine

// assign 종류
//  1 - assign(to: on: )
//  2 - assign(to: )

class MyClass {
    @Published var text: String = "" {
        didSet {
            print("\(oldValue) -> \(text)")
        }
    }
    
    var subscriptions = Set<AnyCancellable>()
    
    init() {
        Timer.publish(every: 1.0, on: .main, in: .common)
            .autoconnect()
            .map { date in
                let formatter = DateFormatter()
                formatter.dateFormat = "HH:mm:ss"
                return formatter.string(from: date)
            }
//            .sink { [weak self] text in
//                self?.text = text
//            }
//            .assign(to: \.text, on: self)
//            .store(in: &subscriptions)
            .assign(to: &self.$text)
            
    }
    
    deinit {
        print("deinit")
    }
}


func runExample() {
    let myClass = MyClass()
    print("runExample 종료")
}

runExample()
