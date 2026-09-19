import UIKit
import Combine

//let url = URL(string: "https://www.naver.com")!
//
//let subscription = URLSession.shared.dataTaskPublisher(for: url)
//    .print("url session publisher")
//    .map { (data: Data, response: URLResponse) in
//        return data
//    }
//    .sink { _ in
//        
//    } receiveValue: { data in
//        print("처리 완료: \(data)")
//    }

//subscription.cancel()

// 이 코드는 동기적으로 작동하기 때문에 subscription할당 없이 동작할 수 있다.
//[1, 2, 3].publisher
//    .sink { num in
//        print(num)
//    }

// delay를 붙임으로서 비동기가 된다.
// 작업을 기다려야 하는 퍼블리셔는 subscription이 꼭 필요하다.
//let subscription2 = [1, 2, 3].publisher
//    .delay(for: 1, scheduler: DispatchQueue.main)
//    .sink { num in
//        print(num)
//    }

//-- class 내부에서의 subscription

class TestClass {
    
//    var subscription: AnyCancellable
//    var subscription2: AnyCancellable
    var cacellabels: Set<AnyCancellable> = []
    
    init() {
        print("init")
        
        let url = URL(string: "https://www.naver.com")!
        
        URLSession.shared.dataTaskPublisher(for: url)
            .print("urlsession publisher1")
            .map { (data: Data, response: URLResponse) in
                return data
            }
            .sink { _ in
        
            } receiveValue: { data in
                print("처리 완료: \(data)")
            }
            .store(in: &cacellabels)
        
        URLSession.shared.dataTaskPublisher(for: url)
            .print("urlsession publisher2")
            .map { (data: Data, response: URLResponse) in
                return data
            }
            .sink { _ in
        
            } receiveValue: { data in
                print("처리 완료: \(data)")
            }
            .store(in: &cacellabels)

    }
}

let testClass = TestClass()
