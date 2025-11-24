// src/components/Common/MyPage.tsx
import React, { useEffect, useMemo, useState } from "react";
import { Card, CardHeader, CardTitle, CardContent } from "../ui/card";
import { Button } from "../ui/button";
import { Label } from "../ui/label";
import { Input } from "../ui/input";
import { toast } from "sonner";
import { User, Mail, Phone, Link2, Edit3 } from "lucide-react";
import api from "../../lib/authApi";
import { useNavigate } from "react-router-dom";

const BACKEND_BASE_URL = import.meta.env.VITE_BACKEND_API_BASE_URL;
const DEFAULT_PROFILE_IMAGE = "/images/default-profile.png";

interface MyPageDTO  {
  id: number;
  name: string;
  email: string;
  phone: string;
  memberImagePath: string | null;
}

interface MyPageProps {
  onProfileImageChange?: (path: string | null) => void;
}

export function MyPage({ onProfileImageChange }: MyPageProps) {
  const navigate = useNavigate();
  const [user, setUser] = useState<MyPageDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);

  // 수정 폼 상태
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");

  // 이미지 미리보기/업로드 파일
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [imageFile, setImageFile] = useState<File | null>(null);

  // 비밀번호 변경
  const [currentPassword, setCurrentPassword] = useState("");
  const [passwordVerified, setPasswordVerified] = useState(false);
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [withdrawing, setWithdrawing] = useState(false);
  const passwordMatch = useMemo(() => {
    if (!newPassword && !confirmPassword) return null;
    return newPassword === confirmPassword ? "일치" : "불일치";
  }, [newPassword, confirmPassword]);

  const staticRoot = BACKEND_BASE_URL.replace(/\/api\/?$/, "");
  const profileImage =
    previewUrl ||
    (user?.memberImagePath
      ? `${staticRoot}/uploads/profile/${user.memberImagePath}`
      : DEFAULT_PROFILE_IMAGE);

  // 초기 로드
  useEffect(() => {
    let alive = true;
    setLoading(true);
    api.get<MyPageDTO>("/api/myPage")
      .then(res => {
        if (!alive) return;
        setUser(res.data);
        setName(res.data.name || "");
        setPhone(res.data.phone || "");
        setPreviewUrl(null);
        setImageFile(null);
        onProfileImageChange?.(res.data.memberImagePath ?? null);
      })
      .catch(err => {
        const msg = err?.response?.data?.message || "마이페이지 정보를 불러오지 못했습니다.";
        toast.error(msg);
      })
      .finally(() => alive && setLoading(false));
    return () => { alive = false; };
  }, []);

  // 이미지 선택
  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (!f) return;
    setImageFile(f);
    const url = URL.createObjectURL(f);
    setPreviewUrl(url);
  };

  // 이미지 기본값으로 초기화
  const handleResetImage = async () => {
    if (!user) return;

    // 아직 저장 안 한 로컬 변경 먼저 리셋
    setPreviewUrl(null);
    setImageFile(null);

    try {
      await api.delete("/api/myPage/profile-image");

      // 로컬 상태도 바로 반영
      setUser((prev) => (prev ? { ...prev, memberImagePath: null } : prev));

      onProfileImageChange?.(null);

      toast.success("기본 프로필 이미지로 변경되었습니다.");
    } catch (e: any) {
      toast.error(
        e?.response?.data?.message || "프로필 이미지를 초기화하는 중 오류가 발생했습니다."
      );
    }
  };


  // 현재 비밀번호 검증
  const handleVerifyPassword = async () => {
    if (!currentPassword.trim()) {
      toast.error("현재 비밀번호를 입력하세요.");
      return;
    }

    try {
      // ✅ 맞으면 200 OK
      await api.post("/api/myPage/check-password", {
        currentPassword,
      });

      setPasswordVerified(true);
      toast.success("비밀번호 확인이 완료되었습니다.");
    } catch (e: any) {
      // ✅ 틀리면 400 → 여기로 들어옴
      setPasswordVerified(false);  // 새 비밀번호 입력란 안 나오게
      const msg =
        e?.response?.data?.message || "현재 비밀번호가 올바르지 않습니다.";
      toast.error(msg);
    }
  };

  // 탈퇴 
  const handleWithdraw = async () => {
    if (!passwordVerified) {
      toast.error("먼저 현재 비밀번호를 확인해 주세요.");
      return;
    }

    const ok = window.confirm(
      "정말로 탈퇴하시겠습니까?\n탈퇴 후에는 동일 계정으로 다시 로그인할 수 없습니다."
    );
    if (!ok) return;

    try {
      setWithdrawing(true);

      await api.delete("/api/myPage");

      // 토큰 정리
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      delete api.defaults.headers.common.Authorization;

      toast.success("회원 탈퇴가 완료되었습니다.");
      navigate("/login", { replace: true });
    } catch (err: any) {
      console.error(err);
      const msg =
        err?.response?.data?.message ??
        "회원 탈퇴 처리 중 오류가 발생했습니다.";
      toast.error(msg);
    } finally {
      setWithdrawing(false);
    }
  };

  // 저장
  const handleSave = async () => {
    if (!user) return;

    // 비밀번호 일치 검사
    if (passwordVerified && newPassword && newPassword !== confirmPassword) {
      toast.error("새 비밀번호가 일치하지 않습니다.");
      return;
    }

    try {
      // 1) 기본 정보 업데이트
      await api.put("/api/myPage", { 
        name, 
        phone,
      });

      // 2) 프로필 이미지 업로드(선택)
      if (imageFile) {
        const fd = new FormData();
        fd.append("file", imageFile);   // 백엔드 @RequestPart("file") 와 이름 맞춤

        await api.post<MyPageDTO>("/api/myPage/profile-image", fd, {
          headers: { "Content-Type": "multipart/form-data" },
        });
      }

      // 3) 비밀번호 변경
      if (passwordVerified && newPassword) {
        await api.post("/api/myPage/change-password", {
          currentPassword,
          newPassword,
        });
      }

      // 4) 최신 데이터 재조회
      const res = await api.get<MyPageDTO>("/api/myPage");
      setUser(res.data);
      setName(res.data.name || "");
      setPhone(res.data.phone || "");
      setPreviewUrl(null);
      setImageFile(null);
      onProfileImageChange?.(res.data.memberImagePath ?? null);

      // 폼 상태 리셋
      setEditing(false);
      setPasswordVerified(false);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");

      toast.success("정보가 변경되었습니다.");
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "저장 중 오류가 발생했습니다.");
    }
  };

   const handleCancel = () => {
    if (!user) return;
    setEditing(false);
    setName(user.name || "");
    setPhone(user.phone || "");
    setPreviewUrl(null);
    setImageFile(null);
    setCurrentPassword("");
    setPasswordVerified(false);
    setNewPassword("");
    setConfirmPassword("");
  };
  
  if (loading) return <div>로딩중…</div>;
  if (!user) return <div>데이터가 없습니다.</div>;

  return (
    <div className="flex flex-col gap-10 pb-20">
      {/* 상단 타이틀 */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h2 className="text-xl font-semibold">마이페이지</h2>
          <p className="text-sm text-gray-500">회원 정보 확인 및 수정</p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg font-semibold">
            <User className="w-5 h-5 text-sky-600" />
            회원 프로필
          </CardTitle>
        </CardHeader>

        <CardContent className="space-y-6">
          {/* 프로필 이미지 */}
          <div className="flex flex-col items-center">
            <img
              src={profileImage}
              alt="프로필 이미지"
              className="w-32 h-32 rounded-full object-cover border border-gray-200 shadow-sm mb-2"
              onError={(e) => {
                e.currentTarget.src = DEFAULT_PROFILE_IMAGE;
              }}
            />

            {editing && (
              <div className="flex gap-2">
                <label className="cursor-pointer px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 text-sm transition">
                  프로필 변경
                  <input
                    type="file"
                    accept="image/*"
                    className="hidden"
                    onChange={handleImageChange}
                  />
                </label>

                {user?.memberImagePath && (
                  <Button
                    type="button"
                    variant="outline"
                    className="text-xs"
                    onClick={handleResetImage}
                  >
                    기본 이미지
                  </Button>
                )}
              </div>
            )}

            {editing ? (
              <div className="mt-3 w-full max-w-xs">
                <Label className="sr-only">이름</Label>
                <Input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="text-center"
                  placeholder="이름을 입력하세요"
                />
              </div>
            ) : (
              <p className="mt-3 text-gray-700 font-medium">{user.name}</p>
            )}
          </div>

          {/* 이메일 / 전화번호 */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
            <div className="space-y-1">
              <Label className="text-gray-600 flex items-center gap-1">
                <Mail className="w-4 h-4 text-gray-400" /> 이메일
              </Label>
              <Input type="text" value={user.email} readOnly />
            </div>

            <div className="space-y-1">
              <Label className="text-gray-600 flex items-center gap-1">
                <Phone className="w-4 h-4 text-gray-400" /> 전화번호
              </Label>
              {editing ? (
                <Input value={phone} onChange={(e) => setPhone(e.target.value)} />
              ) : (
                <Input type="text" value={phone} readOnly />
              )}
            </div>
          </div>

          {/* 비밀번호 변경 */}
          {editing && (
            <div className="mt-4 space-y-2">
              <Label>현재 비밀번호 확인</Label>
              <div className="flex gap-2">
                <Input
                  type="password"
                  value={currentPassword}
                  onChange={(e) => {
                    setCurrentPassword(e.target.value);
                    setPasswordVerified(false);  // 다시 확인해야만 새 비번 입력란 보이게
                  }}
                />
                <Button
                  type="button"
                  onClick={handleVerifyPassword}
                  className="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600 transition"
                >
                  확인
                </Button>
              </div>

              {passwordVerified && (
                <>
                  <Label>새 비밀번호</Label>
                  <Input
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                  />
                  <Label>새 비밀번호 확인</Label>
                  <Input
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                  />
                  {passwordMatch && (
                    <p className={`text-sm ${passwordMatch === "일치" ? "text-green-600" : "text-red-600"}`}>
                      {passwordMatch === "일치" ? "비밀번호가 일치합니다." : "비밀번호가 일치하지 않습니다."}
                    </p>
                  )}
                </>
              )}
            </div>
          )}

          {/* 버튼 그룹 */}
          <div className="flex justify-between mt-6 items-center">
            {editing && passwordVerified && (
              <Button
                type="button"
                className="px-3 py-1 bg-gray-200 text-gray-600 border border-gray-300 rounded text-xs"
                onClick={handleWithdraw}
                disabled={withdrawing}
              >
                {withdrawing ? "탈퇴 처리 중..." : "회원 탈퇴"}
              </Button>
            )}
            
            <div className="flex gap-2 ml-auto">
              {editing ? (
                <>
                  <Button className="bg-gray-300 hover:bg-gray-400" onClick={handleCancel}>
                    취소
                  </Button>
                  <Button className="bg-orange-500 hover:bg-orange-600 text-white" onClick={handleSave}>
                    저장
                  </Button>
                </>
              ) : (
                <Button
                  className="bg-orange-500 hover:bg-orange-600 text-white flex items-center gap-2"
                  onClick={() => setEditing(true)}
                >
                  <Edit3 className="w-4 h-4" /> 정보 수정
                </Button>
              )}
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
