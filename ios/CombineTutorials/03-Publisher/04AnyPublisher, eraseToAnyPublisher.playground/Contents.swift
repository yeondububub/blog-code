import UIKit
import Combine


//func publisher1() -> Publishers.Sequence<[Int], Never> {
//    let pub = [1, 2, 3].publisher
//    return pub
//}

func publisher1() -> AnyPublisher<Int, Never> {
    let pub = [1, 2, 3].publisher
        .eraseToAnyPublisher()
    return pub
}

func publisher2() ->  AnyPublisher<Data, URLError> {
    let pub = URLSession.shared.dataTaskPublisher(for: URL(string: "https://www.naver.com")!)
        .map(\.data) //keypath방식
//        .map { (data: Data, response: URLResponse) in //transform방식1
//            return data
//        }
//        .map { (data: Data, response: URLResponse) in //transform방식2
//            data
//        }
//        .map { return $0 } //transform방식3
//        .map { $0 } //transform방식4
        .eraseToAnyPublisher()
    
    return pub
}


publisher1()
    .sink { num in
        print(num)
    }


let subscription = publisher2()
    .sink { _ in
    } receiveValue: { data in
        print(data)
    }

