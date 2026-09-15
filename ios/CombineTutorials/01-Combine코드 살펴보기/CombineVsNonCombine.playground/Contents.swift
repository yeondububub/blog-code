//non-Combine, 일반적인 비동기 코드(completion handler)
import Foundation

struct CatFact: Decodable {
    let fact: String
    let length: Int
}

func fetchCatFact(completion: @escaping (CatFact) -> Void) {
    let url = URL(string: "https://catfact.ninja/fact")!

    let task = URLSession.shared.dataTask(with: url) { data, response, error in
        let decoder = JSONDecoder()
        let catFact = try! decoder.decode(CatFact.self, from: data!)
        completion(catFact)
    }
    task.resume()
}

fetchCatFact { catFactString in
    DispatchQueue.main.async {
        print(catFactString)
    }
}



//컴바인 적용 후
import Foundation
import Combine

//struct CatFact: Decodable {
//    let fact: String
//    let length: Int
//}

func fetchCatFactPublisher() -> AnyPublisher<CatFact, Error> {
    let url = URL(string: "https://catfact.ninja/fact")!

    return URLSession.shared.dataTaskPublisher(for: url)
        .map { (data: Data, response: URLResponse) in
            return data
        }
        .decode(type: CatFact.self, decoder: JSONDecoder())
        .receive(on: DispatchQueue.main)
        .eraseToAnyPublisher()
}


let subscription = fetchCatFactPublisher()
    .sink { completion in
        print(completion)
    } receiveValue: { catFact in
        print(catFact)
    }
