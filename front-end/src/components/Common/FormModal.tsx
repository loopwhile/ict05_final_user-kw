import React from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '../ui/dialog';
import { Button } from '../ui/button';

interface FormField {
  name: string;
  label: string;
  type:
    | 'text'
    | 'email'
    | 'password'
    | 'number'
    | 'select'
    | 'textarea'
    | 'date'
    | 'file'
    | 'tel'
    | 'month'
    | 'time';
  placeholder?: string;
  required?: boolean;
  options?: { value: string; label: string }[];
  validation?: (value: unknown) => string | undefined;
}

interface FormModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  fields: FormField[];
  onSubmit: (data: Record<string, unknown>) => void;
  initialData?: Record<string, unknown>;
  submitText?: string;
  cancelText?: string;
  isLoading?: boolean;
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl';
  onChange?: (fieldName: string, value: unknown, formData: Record<string, unknown>) => void;
}

// 폼 값용 타입(키-값 자유형)
type FormValues = Record<string, unknown>;

export function FormModal({
  isOpen,
  onClose,
  title = '양식',
  fields = [],
  onSubmit,
  initialData = {},
  submitText = '저장',
  cancelText = '취소',
  isLoading = false,
  maxWidth = 'md',
  onChange,
}: FormModalProps) {
  // ✅ formData를 명확히 타입 선언
  const [formData, setFormData] = React.useState<FormValues>({});
  const [errors, setErrors] = React.useState<Record<string, string>>({});

  // fields 방어적 필터
  const validFields = React.useMemo(() => {
    if (!Array.isArray(fields)) return [];
    return fields.filter((field) => field && typeof field === 'object' && field.name && field.label && field.type);
  }, [fields]);

  // 모달 열릴 때/닫힐 때 초기화
  React.useEffect(() => {
    if (isOpen) {
      const safeInitialData = initialData && typeof initialData === 'object' ? initialData : {};
      setFormData(safeInitialData as FormValues);
      setErrors({});
    } else {
      setFormData({});
      setErrors({});
    }
  }, [isOpen]); // initialData 의존성 제거

  // initialData가 실제로 바뀌었을 때만 병합
  const initialDataString = React.useMemo(() => JSON.stringify(initialData || {}), [initialData]);

  React.useEffect(() => {
    if (isOpen && initialData) {
      const safeInitialData = initialData && typeof initialData === 'object' ? initialData : {};
      // ✅ 업데이터 콜백의 prev 타입 지정
      setFormData((prev: FormValues) => ({ ...prev, ...(safeInitialData as FormValues) }));
    }
  }, [initialDataString, isOpen]);

  const handleChange = (name: string, value: unknown) => {
    const newFormData: FormValues = { ...formData, [name]: value };
    setFormData(newFormData);

    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }

    const field = validFields.find((f) => f.name === name);
    if (field?.validation && value !== undefined && value !== null && value !== '') {
      const validationError = field.validation(value);
      if (validationError) {
        setErrors((prev) => ({ ...prev, [name]: validationError }));
      }
    }

    onChange?.(name, value, newFormData);
  };

  const validateForm = () => {
    const newErrors: Record<string, string> = {};

    validFields.forEach((field) => {
      const value = formData[field.name];

      if (field.required && (value === undefined || value === null || (typeof value === 'string' && !value.trim()))) {
        newErrors[field.name] = `${field.label}은(는) 필수 입력 항목입니다.`;
        return;
      }
      if (field.validation && value !== undefined && value !== null && value !== '') {
        const error = field.validation(value);
        if (error) newErrors[field.name] = error;
      }
    });

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (validateForm()) onSubmit(formData);
  };

  const renderField = (field: FormField) => {
    const raw = formData[field.name];
    const value = (raw ?? '') as string; // input 계열은 string 처리
    const hasError = !!errors[field.name];

    switch (field.type) {
      case 'select':
        return (
          <select
            id={field.name}
            value={value}
            onChange={(e) => handleChange(field.name, e.target.value)}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-kpi-red focus:border-transparent ${
              hasError ? 'border-red-500' : 'border-gray-300'
            }`}
            required={field.required}
          >
            <option value="">{field.placeholder || `${field.label} 선택`}</option>
            {field.options?.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        );

      case 'textarea':
        return (
          <textarea
            id={field.name}
            value={value}
            onChange={(e) => handleChange(field.name, e.target.value)}
            placeholder={field.placeholder}
            rows={4}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-kpi-red focus:border-transparent resize-none ${
              hasError ? 'border-red-500' : 'border-gray-300'
            }`}
            required={field.required}
          />
        );

      case 'file':
        return (
          <input
            id={field.name}
            type="file"
            onChange={(e) => handleChange(field.name, e.target.files?.[0] ?? null)}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-kpi-red focus:border-transparent ${
              hasError ? 'border-red-500' : 'border-gray-300'
            }`}
            required={field.required}
          />
        );

      case 'month':
        return (
          <input
            id={field.name}
            type="month"
            value={value}
            onChange={(e) => handleChange(field.name, e.target.value)}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-kpi-red focus:border-transparent ${
              hasError ? 'border-red-500' : 'border-gray-300'
            }`}
            required={field.required}
          />
        );

      case 'number':
        return (
          <input
            id={field.name}
            type="number"
            value={value}
            onChange={(e) => handleChange(field.name, e.target.value)}
            placeholder={field.placeholder}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-kpi-red focus:border-transparent ${
              hasError ? 'border-red-500' : 'border-gray-300'
            }`}
            required={field.required}
          />
        );

      case 'time':
        return (
          <input
            id={field.name}
            type="time"
            value={value}
            onChange={(e) => handleChange(field.name, e.target.value)}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-kpi-red focus:border-transparent ${
              hasError ? 'border-red-500' : 'border-gray-300'
            }`}
            required={field.required}
          />
        );

      default:
        return (
          <input
            id={field.name}
            type={field.type}
            value={value}
            onChange={(e) => handleChange(field.name, e.target.value)}
            placeholder={field.placeholder}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-kpi-red focus:border-transparent ${
              hasError ? 'border-red-500' : 'border-gray-300'
            }`}
            required={field.required}
          />
        );
    }
  };

  const maxWidthClasses = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
    '2xl': 'max-w-2xl',
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className={`${maxWidthClasses[maxWidth]} max-h-[90vh] overflow-y-auto`}>
        <DialogHeader>
          <DialogTitle className="text-lg font-semibold text-gray-900">{title}</DialogTitle>
          <DialogDescription className="sr-only">폼을 작성하여 데이터를 입력하거나 수정할 수 있습니다.</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          {validFields.map((field) => (
            <div key={field.name} className="space-y-2">
              <label htmlFor={field.name} className="text-sm font-medium text-gray-700">
                {field.label}
                {field.required && <span className="text-red-500 ml-1">*</span>}
              </label>

              {renderField(field)}

              {errors[field.name] && <p className="text-sm text-red-600">{errors[field.name]}</p>}
            </div>
          ))}

          <div className="flex justify-end gap-3 pt-4 border-t">
            <Button type="button" variant="outline" onClick={onClose} disabled={isLoading}>
              {cancelText}
            </Button>
            <Button type="submit" className="bg-kpi-red hover:bg-red-600 text-white" disabled={isLoading}>
              {isLoading ? '처리중...' : submitText}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
