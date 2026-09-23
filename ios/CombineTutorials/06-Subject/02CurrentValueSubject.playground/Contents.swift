import UIKit
import Combine

//let subject = PassthroughSubject<Int, Never>()
let subject = CurrentValueSubject<Int, Never>(0)

let subscription = subject
    .sink { number in
        print("숫자 \(number)가 서브젝트로 전달되었습니다.")
    }

import SwiftUI
import PlaygroundSupport

struct ContentView: View {
    var body: some View {
        Button("Click") {
            let randomNumber = Int.random(in: 0...10)
            //subject.send(randomNumber)
            
            let sum = subject.value + randomNumber
            subject.send(sum)
            
            
        }
    }
}

PlaygroundPage.current.setLiveView(ContentView())
