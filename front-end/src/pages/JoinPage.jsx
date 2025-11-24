// import { useState, useEffect } from "react";
// import { useNavigate } from "react-router-dom";
// import "../styles/LoginPage.css";

// const BASE = process.env.REACT_APP_BACKEND_API_BASE_URL;

// export default function JoinPage() {
//   const navigate = useNavigate();

//   const [email, setEmail] = useState("");
//   const [isEmailValid, setIsEmailValid] = useState(null); // null | true | false

//   const [password, setPassword] = useState("");
//   const [password2, setPassword2] = useState("");

//   const [name, setName] = useState("");
//   const [phone, setPhone] = useState("");

//   const [loading, setLoading] = useState(false);
//   const [error, setError] = useState("");
//   const [ok, setOk] = useState("");

//   // === 이메일 중복 체크 ===
//   useEffect(() => {
//     if (!BASE || !email || email.length < 5) {
//       setIsEmailValid(null);
//       return;
//     }
//     const t = setTimeout(async () => {
//       try {
//         const res = await fetch(`${BASE}/user/exist`, {
//           method: "POST",
//           headers: { "Content-Type": "application/json" },
//           credentials: "include",
//           body: JSON.stringify({ email }),
//         });
//         const exists = await res.json();
//         setIsEmailValid(!exists);
//       } catch {
//         setIsEmailValid(null);
//       }
//     }, 300);
//     return () => clearTimeout(t);
//   }, [email]);

//   // === 비밀번호 유효성/일치 여부 계산 ===
//   const pwMin = 4;
//   const isPwLenOk = password.length >= pwMin;
//   const isPwSame = password2.length > 0 && password === password2;

//   // 제출
//   const handleSignUp = async (e) => {
//     e.preventDefault();
//     if (loading) return;

//     setError("");
//     setOk("");

//     if (!BASE) return setError("백엔드 BASE URL이 설정되지 않았습니다.");
//     if (!email.trim()) return setError("이메일을 입력하세요.");
//     if (isEmailValid === false) return setError("이미 사용 중인 이메일입니다.");
//     if (!isPwLenOk) return setError(`비밀번호는 ${pwMin}자 이상이어야 합니다.`);
//     if (!isPwSame) return setError("비밀번호가 일치하지 않습니다.");
//     if (!name.trim()) return setError("이름을 입력하세요.");

//     const payload = { email, password, name, phone };

//     try {
//       setLoading(true);
//       const res = await fetch(`${BASE}/user`, {
//         method: "POST",
//         headers: { "Content-Type": "application/json" },
//         credentials: "include",
//         body: JSON.stringify(payload),
//       });

//       if (!res.ok) {
//         setError(`회원가입 실패 (${res.status})`);
//         return;
//       }

//       setOk("회원가입이 완료되었습니다. 로그인 화면으로 이동합니다.");
//       setTimeout(() => navigate("/login"), 600);
//     } catch {
//       setError("회원가입 중 오류가 발생했습니다.");
//     } finally {
//       setLoading(false);
//     }
//   };

//   return (
//     <div className="login-wrap">
//       <div className="login-card">
//         <div className="login-logo">📑</div>
//         <h1 className="login-title">회원가입</h1>
//         <p className="login-subtitle">FranFriend ERP 계정을 생성하세요</p>

//         <form onSubmit={handleSignUp} style={{ marginTop: 8 }}>
//           {/* 이메일 */}
//           <label className="login-label">이메일 *</label>
//           <input
//             className="login-input"
//             type="email"
//             placeholder="email@example.com"
//             value={email}
//             onChange={(e) => setEmail(e.target.value)}
//             required
//           />
//           {email && isEmailValid === false && (
//             <p className="login-error">이미 사용 중인 이메일입니다.</p>
//           )}
//           {email && isEmailValid === true && (
//             <p className="login-hint-ok">사용 가능한 이메일입니다.</p>
//           )}

//           {/* 비밀번호 */}
//           <label className="login-label">비밀번호 *</label>
//           <input
//             className="login-input"
//             type="password"
//             placeholder={`비밀번호 (${pwMin}자 이상)`}
//             value={password}
//             onChange={(e) => setPassword(e.target.value)}
//             required
//             minLength={pwMin}
//           />
//           {password && !isPwLenOk && (
//             <p className="login-error">비밀번호는 {pwMin}자 이상이어야 합니다.</p>
//           )}
//           {password && isPwLenOk && (
//             <p className="login-hint-ok">사용 가능한 비밀번호입니다.</p>
//           )}

//           {/* 비밀번호 확인 */}
//           <label className="login-label">비밀번호 확인 *</label>
//           <input
//             className="login-input"
//             type="password"
//             placeholder="비밀번호 재입력"
//             value={password2}
//             onChange={(e) => setPassword2(e.target.value)}
//             required
//             minLength={pwMin}
//           />
//           {password2 && !isPwSame && (
//             <p className="login-error">비밀번호가 일치하지 않습니다.</p>
//           )}
//           {password2 && isPwSame && (
//             <p className="login-hint-ok">비밀번호가 일치합니다.</p>
//           )}

//           {/* 이름 */}
//           <label className="login-label">이름 *</label>
//           <input
//             className="login-input"
//             type="text"
//             placeholder="이름"
//             value={name}
//             onChange={(e) => setName(e.target.value)}
//             required
//           />

//           {/* 연락처(선택) */}
//           <label className="login-label">연락처</label>
//           <input
//             className="login-input"
//             type="text"
//             placeholder="010-0000-0000"
//             value={phone}
//             onChange={(e) => setPhone(e.target.value)}
//           />

//           {error && <p className="login-error">{error}</p>}
//           {ok && <p className="login-hint-ok">{ok}</p>}

//           <button
//             type="submit"
//             className="login-btn-primary"
//             disabled={
//               loading ||
//               isEmailValid === false ||
//               !isPwLenOk ||
//               !isPwSame
//             }
//           >
//             {loading ? "생성 중..." : "계정 생성"}
//           </button>

//           <button
//             type="button"
//             onClick={() => navigate("/login")}
//             className="login-btn-secondary"
//           >
//             로그인으로 돌아가기
//           </button>
//         </form>

//         <footer className="login-footer">
//           © 2024 FranFriend ERP. All rights reserved.
//         </footer>
//       </div>
//     </div>
//   );
// }
