import UIKit
import Combine

let subject = PassthroughSubject<Int, URLError>()

let subscription = subject.sink { completion in
    switch completion {
    case .finished:
        print("정상완료")
    case .failure(let error):
        print("에러발생 \(error)")
    }
} receiveValue: { number in
    print("숫자 \(number)가 서브젝트로 전송되었습니다.")
}

subject.send(1)
subject.send(2)
//subject.send(completion: .finished)
//subject.send(completion: .failure(URLError(.badURL)))

import SwiftUI
import PlaygroundSupport

struct ContentView: View {
    var body: some View {
        Button("Click"){
            let randomNumber = Int.random(in: 0...10)
            subject.send(randomNumber)
        }
    }
}

PlaygroundPage.current.setLiveView(ContentView())
