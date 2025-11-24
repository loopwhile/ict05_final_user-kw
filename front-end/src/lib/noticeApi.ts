import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_BACKEND_API_BASE_URL;
const API_URL = `${API_BASE_URL}/api/notice`; // Spring Boot 서버의 주소

// 페이지 정보 타입을 정의합니다.
export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort: {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
  };
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

// 공지사항 목록 아이템 타입을 정의합니다.
export interface Notice {
  id: number;
  memberIdFk: number;
  noticeCategory: string;
  noticePriority: 'NORMAL' | 'IMPORTANT' | 'EMERGENCY';
  noticeStatus: string;
  isShow: boolean;
  title: string;
  body: string;
  writer: string;
  noticeCount: number;
  registeredAt: string; // LocalDateTime은 문자열로 직렬화됩니다.
}

// 공지사항 상세 정보 타입을 정의합니다.
export interface NoticeDetail {
  notice: Notice;
  attachments: Attachment[];
}

// 첨부파일 타입을 정의합니다.
export interface Attachment {
  id: number;
  noticeId: number;
  url: string;
  originalFilename: string;
}

// 공지사항 카운트 정보 타입을 정의합니다.
export interface NoticeCountDTO {
  totalCount: number;
  urgentCount: number;
  importantCount: number;
  unreadCount: number;
}

// 공지사항 목록 응답 DTO 타입을 정의합니다.
export interface NoticeListResponseDTO {
  pageData: Page<Notice>;
  countData: NoticeCountDTO;
}

/**
 * 공지사항 목록을 가져오는 API 함수
 * @param page - 페이지 번호 (1부터 시작)
 * @param size - 페이지당 항목 수
 * @param type - 검색 유형 (title, content, all, writer)
 * @param s - 검색어
 * @param priority - 중요도 필터
 * @returns NoticeListResponseDTO
 */
export const getNoticeList = async (
  page: number,
  size: number,
  type?: string,
  s?: string,
  priority?: 'NORMAL' | 'IMPORTANT' | 'EMERGENCY'
): Promise<NoticeListResponseDTO> => {
  try {
    const response = await axios.get<NoticeListResponseDTO>(`${API_URL}/list`, {
      params: {
        page,
        size,
        type,
        s,
        priority,
      },
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching notice list:', error);
    throw error;
  }
};

/**
 * 공지사항 상세 정보를 가져오는 API 함수
 * @param id - 공지사항 ID
 * @returns NoticeDetail
 */
export const getNoticeDetail = async (id: number): Promise<NoticeDetail> => {
  try {
    const response = await axios.get<NoticeDetail>(`${API_URL}/detail/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching notice detail for id ${id}:`, error);
    throw error;
  }
};
