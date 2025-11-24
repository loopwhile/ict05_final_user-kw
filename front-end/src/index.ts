// // src/index.js (라우팅 전부 포함)
// import React from "react";
// import { createRoot } from "react-dom/client";
// import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
// import "./styles/App.css";

// // import Login from "./components/Common/Login";
// import Login from "./components/Common/Login";
// import { Register } from "./components/Common/Register";
// import App from "./App";

// const root = createRoot(document.getElementById("root"));
// root.render(
//   <BrowserRouter>
//     <Routes>
//       <Route path="/" element={<Navigate to="/login" replace />} />
//       <Route path="/login" element={<LoginPage />} />
//       <Route path="/join" element={<Register />} />
//       <Route path="/" element={<App />} />
//       <Route path="*" element={<Navigate to="/login" replace />} />
//     </Routes>
//   </BrowserRouter>
// );
