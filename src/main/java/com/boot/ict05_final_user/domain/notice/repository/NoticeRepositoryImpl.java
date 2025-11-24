package com.boot.ict05_final_user.domain.notice.repository;

import com.boot.ict05_final_user.domain.notice.dto.NoticeListDTO;
import com.boot.ict05_final_user.domain.notice.dto.NoticeSearchDTO;
import com.boot.ict05_final_user.domain.notice.entity.NoticePriority;
import com.boot.ict05_final_user.domain.notice.entity.QNotice;
import com.boot.ict05_final_user.domain.notice.entity.QNoticeAttachment;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 공지사항 커스텀 리포지토리 구현체.
 *
 * <p>{@link NoticeRepositoryCustom}을 구현하며, QueryDSL 기반의 동적 검색 및 페이징 처리를 수행합니다.</p>
 *
 * <ul>
 *     <li>검색 조건(제목, 내용, 작성자, 전체 키워드 등) 필터링</li>
 *     <li>첨부파일 존재 여부 및 첫 번째 첨부파일 URL 병합 조회</li>
 *     <li>페이징(Pageable) 기반 리스트 반환</li>
 * </ul>
 *
 */
@Repository
@RequiredArgsConstructor
public class NoticeRepositoryImpl implements NoticeRepositoryCustom {

	private final JPAQueryFactory queryFactory;
	private final NoticeAttachmentRepository noticeAttachmentRepository;

	/**
	 * 검색 조건 및 페이징 정보를 기반으로 공지사항 목록을 조회한다.
	 * <p>QueryDSL의 {@link Projections#fields}를 사용하여 DTO로 직접 매핑합니다.</p>
	 *
	 * @param noticeSearchDTO 검색 조건 DTO
	 * @param pageable 페이지 정보
	 * @return 공지사항 리스트 페이지 객체
	 */
	@Override
	public Page<NoticeListDTO> listNotice(NoticeSearchDTO noticeSearchDTO, Pageable pageable) {
		QNotice notice = QNotice.notice;
		QNoticeAttachment attachment = QNoticeAttachment.noticeAttachment;

		BooleanExpression searchPredicate = applySearchFilters(noticeSearchDTO, notice);

		List<NoticeListDTO> content = queryFactory
				.select(Projections.fields(NoticeListDTO.class,
						notice.id,
						notice.memberIdFk,
						notice.noticeCategory,
						notice.noticePriority,
						notice.noticeStatus,
						notice.isShow,
						notice.title,
						notice.body,
						notice.writer,
						notice.noticeCount,
						notice.registeredAt,
						ExpressionUtils.as(
								JPAExpressions.selectOne()
										.from(attachment)
										.where(attachment.noticeId.eq(notice.id))
										.exists(),
								"hasAttachment"
						),
						ExpressionUtils.as(
								JPAExpressions.select(attachment.url)
										.from(attachment)
										.where(attachment.noticeId.eq(notice.id))
										.orderBy(attachment.id.asc())
										.limit(1),
								"firstAttachmentUrl"
						)
				))
				.from(notice)
				.where(searchPredicate)
				.orderBy(notice.id.desc())
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch();

		long total = queryFactory
				.select(notice.count())
				.from(notice)
				.where(searchPredicate)
				.fetchOne();

		return new PageImpl<>(content, pageable, total);
	}

	/** 검색 조건(키워드 + 우선순위) 통합 필터 */
	private BooleanExpression applySearchFilters(NoticeSearchDTO dto, QNotice notice) {
		BooleanExpression keywordExp = eqKeyword(dto, notice);
		BooleanExpression priorityExp = eqPriority(dto.getPriority(), notice);
		if (keywordExp != null && priorityExp != null) return keywordExp.and(priorityExp);
		if (keywordExp != null) return keywordExp;
		return priorityExp;
	}

	/** 키워드 검색 필터 */
	private BooleanExpression eqKeyword(NoticeSearchDTO dto, QNotice notice) {
		String type = dto.getType();
		String keyword = dto.getS();

		if (!StringUtils.hasText(type) || !StringUtils.hasText(keyword)) return null;

		return switch (type) {
			case "title" -> notice.title.containsIgnoreCase(keyword);
			case "content" -> notice.body.containsIgnoreCase(keyword);
			case "writer" -> notice.writer.containsIgnoreCase(keyword);
			case "all" -> notice.title.containsIgnoreCase(keyword)
					.or(notice.body.containsIgnoreCase(keyword))
					.or(notice.writer.containsIgnoreCase(keyword));
			default -> null;
		};
	}

	/** 우선순위 검색 필터 */
	private BooleanExpression eqPriority(NoticePriority priority, QNotice notice) {
		return (priority != null) ? notice.noticePriority.eq(priority) : null;
	}

	/**
	 * 검색 조건 기반의 전체 공지 개수를 조회한다.
	 *
	 * @param noticeSearchDTO 검색 조건 DTO
	 * @return 조건에 해당하는 전체 공지 개수
	 */
	@Override
	public long countNotice(NoticeSearchDTO noticeSearchDTO) {
		QNotice notice = QNotice.notice;
		BooleanExpression predicate = applySearchFilters(noticeSearchDTO, notice);
		return queryFactory.select(notice.count())
				.from(notice)
				.where(predicate)
				.fetchOne();
	}

	/**
	 * 공지 우선순위 기준으로 개수를 조회한다.
	 *
	 * @param priority 공지 우선순위
	 * @return 해당 우선순위의 공지 개수
	 */
	@Override
	public long countByPriority(NoticePriority priority) {
		QNotice notice = QNotice.notice;
		return queryFactory.select(notice.count())
				.from(notice)
				.where(notice.noticePriority.eq(priority))
				.fetchOne();
	}
}
