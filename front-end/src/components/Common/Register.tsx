import React, { useState } from "react";
import { Card } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Users, User, Phone, Mail, Lock, ArrowLeft } from "lucide-react";
import { toast } from "sonner";

interface RegisterProps {
  onRegister: (userData: any) => void;   // ← userType 제거
  onBackToLogin: () => void;
}

export function Register({ onRegister, onBackToLogin }: RegisterProps) {
  const [formData, setFormData] = useState({
    email: "",
    password: "",
    confirmPassword: "",
    name: "",
    phone: "",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleInputChange = (field: string, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) setErrors((prev) => ({ ...prev, [field]: "" }));
  };

  const validateForm = () => {
    const e: Record<string, string> = {};
    if (!formData.name) e.name = "이름을 입력해주세요.";
    if (!formData.phone) e.phone = "연락처를 입력해주세요.";
    if (!formData.email) e.email = "이메일을 입력해주세요.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email))
      e.email = "올바른 이메일 형식을 입력해주세요.";
    if (!formData.password) e.password = "비밀번호를 입력해주세요.";
    else if (formData.password.length < 6)
      e.password = "비밀번호는 6자 이상이어야 합니다.";
    if (!formData.confirmPassword)
      e.confirmPassword = "비밀번호 확인을 입력해주세요.";
    else if (formData.password !== formData.confirmPassword)
      e.confirmPassword = "비밀번호가 일치하지 않습니다.";

    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;

    const userData = {
      email: formData.email,
      password: formData.password,
      name: formData.name,
      phone: formData.phone,
      registeredAt: new Date().toISOString(),
    };

    onRegister(userData);            // ← userType 없이 전달
    toast.success("회원가입이 완료되었습니다!");
  };

  return (
    <div className="min-h-screen bg-light-gray flex items-center justify-center p-4">
      <Card className="w-full max-w-lg p-8 bg-white rounded-xl shadow-lg">
        {/* Header */}
        <div className="flex items-center mb-6">
          <Button variant="ghost" size="sm" onClick={onBackToLogin} className="mr-2 p-2">
            <ArrowLeft className="w-4 h-4" />
          </Button>
          <div className="flex-1 text-center">
            <div className="w-12 h-12 bg-kpi-purple rounded-xl flex items-center justify-center mx-auto mb-3">
              <Users className="w-6 h-6 text-white" />
            </div>
            <h1 className="text-xl font-bold text-gray-900">회원가입</h1>
            <p className="text-sm text-dark-gray">FranFriend ERP 계정을 생성하세요</p>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label className="text-sm font-medium text-gray-700 mb-2 block">
                이름 <span className="text-red-500">*</span>
              </Label>
              <div className="relative">
                <User className="w-4 h-4 text-dark-gray absolute left-3 top-1/2 transform -translate-y-1/2" />
                <Input
                  type="text"
                  placeholder="이름"
                  value={formData.name}
                  onChange={(e) => handleInputChange("name", e.target.value)}
                  className={`pl-9 h-10 ${errors.name ? "border-red-500" : "border-gray-300"}`}
                />
              </div>
              {errors.name && <p className="text-xs text-red-600 mt-1">{errors.name}</p>}
            </div>

            <div>
              <Label className="text-sm font-medium text-gray-700 mb-2 block">
                연락처 <span className="text-red-500">*</span>
              </Label>
              <div className="relative">
                <Phone className="w-4 h-4 text-dark-gray absolute left-3 top-1/2 transform -translate-y-1/2" />
                <Input
                  type="tel"
                  placeholder="010-0000-0000"
                  value={formData.phone}
                  onChange={(e) => handleInputChange("phone", e.target.value)}
                  className={`pl-9 h-10 ${errors.phone ? "border-red-500" : "border-gray-300"}`}
                />
              </div>
              {errors.phone && <p className="text-xs text-red-600 mt-1">{errors.phone}</p>}
            </div>
          </div>

          <div>
            <Label className="text-sm font-medium text-gray-700 mb-2 block">
              이메일 <span className="text-red-500">*</span>
            </Label>
            <div className="relative">
              <Mail className="w-4 h-4 text-dark-gray absolute left-3 top-1/2 transform -translate-y-1/2" />
              <Input
                type="email"
                placeholder="email@example.com"
                value={formData.email}
                onChange={(e) => handleInputChange("email", e.target.value)}
                className={`pl-9 h-10 ${errors.email ? "border-red-500" : "border-gray-300"}`}
              />
            </div>
            {errors.email && <p className="text-xs text-red-600 mt-1">{errors.email}</p>}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label className="text-sm font-medium text-gray-700 mb-2 block">
                비밀번호 <span className="text-red-500">*</span>
              </Label>
              <div className="relative">
                <Lock className="w-4 h-4 text-dark-gray absolute left-3 top-1/2 transform -translate-y-1/2" />
                <Input
                  type="password"
                  placeholder="비밀번호"
                  value={formData.password}
                  onChange={(e) => handleInputChange("password", e.target.value)}
                  className={`pl-9 h-10 ${errors.password ? "border-red-500" : "border-gray-300"}`}
                />
              </div>
              {errors.password && <p className="text-xs text-red-600 mt-1">{errors.password}</p>}
            </div>

            <div>
              <Label className="text-sm font-medium text-gray-700 mb-2 block">
                비밀번호 확인 <span className="text-red-500">*</span>
              </Label>
              <div className="relative">
                <Lock className="w-4 h-4 text-dark-gray absolute left-3 top-1/2 transform -translate-y-1/2" />
                <Input
                  type="password"
                  placeholder="비밀번호 확인"
                  value={formData.confirmPassword}
                  onChange={(e) => handleInputChange("confirmPassword", e.target.value)}
                  className={`pl-9 h-10 ${errors.confirmPassword ? "border-red-500" : "border-gray-300"}`}
                />
              </div>
              {errors.confirmPassword && (
                <p className="text-xs text-red-600 mt-1">{errors.confirmPassword}</p>
              )}
            </div>
          </div>

          <div className="pt-4">
            <Button type="submit" className="w-full h-11 rounded-lg font-medium bg-kpi-green hover:bg-green-600 text-white">
              계정 생성
            </Button>
          </div>
        </form>

        <div className="mt-6 pt-4 border-t border-gray-200 text-center">
          <p className="text-xs text-dark-gray">
            계정 생성 시
            <a href="#" className="text-kpi-red hover:underline ml-1">이용약관</a> 및
            <a href="#" className="text-kpi-red hover:underline ml-1">개인정보처리방침</a>에 동의하게 됩니다.
          </p>
        </div>
      </Card>
    </div>
  );
}
