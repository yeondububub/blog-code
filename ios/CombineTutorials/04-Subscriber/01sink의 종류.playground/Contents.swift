import Combine
import Foundation

let arrayPublisher = [1, 2, 3, 4].publisher
    // 다음 publisher의 경우 sink가 2개가 나온다.
    // .sink(receiveValue: <#T##(Int) -> Void#>)
    // .sink(receiveCompletion: <#T##(Subscribers.Completion<Never>) -> Void#>, receiveValue: <#T##(Int) -> Void#>)

let urlPublisher = URLSession.shared.dataTaskPublisher(for: URL(string: "https://www.naver.com/")!)

// 다음 publisher의 경우 sink가 1개가 나온다
// .sink(receiveCompletion: <#T##(Subscribers.Completion<URLError>) -> Void#>, receiveValue: <#T##((data: Data, response: URLResponse)) -> Void#>)

// sink가 2개가 나오는 이유는 에러가 발생하지 않기 때문에 completion이 중요하지 않다.
// 하지만 에러가 발생하는 publisher의 경우에는 Completion이 중요하기 때문에 completion이 들어가는 sink를 반드시 써야 한다.
