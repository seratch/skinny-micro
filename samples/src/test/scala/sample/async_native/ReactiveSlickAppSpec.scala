// package sample.async_native
//
// import skinny.jackson.JSONStringOps
// import skinny.test.SkinnyFunSpec
//
// class ReactiveSlickAppSpec extends SkinnyFunSpec with JSONStringOps {
//   addFilter(classOf[ReactiveSlickApp], "/*")
//
//   describe("Reactive Slick Demo") {
//
//     it("shows coffees") {
//       get("/slick-demo/coffees") {
//         if (status != 200) println(body)
//         status should equal(200)
//         header("Content-Type") should equal("application/json;charset=utf-8")
//       }
//     }
//
//     it("shows suppliers") {
//       get("/slick-demo/suppliers") {
//         if (status != 200) println(body)
//         status should equal(200)
//         header("Content-Type") should equal("application/json;charset=utf-8")
//       }
//     }
//   }
//
// }
