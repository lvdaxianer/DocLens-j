package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * 文档删除用例测试仓储集合。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class DocumentDeleteUseCaseRepositories {

    /** 文档仓储桩。 */
    final DeleteUseCaseDocumentJobRepository documentRepository = new DeleteUseCaseDocumentJobRepository();
    /** 批次仓储桩。 */
    final DeleteUseCaseBatchRepository batchRepository = new DeleteUseCaseBatchRepository();
    /** 结果仓储桩。 */
    final DeleteUseCaseOcrResultRepository resultRepository = new DeleteUseCaseOcrResultRepository();
    /** 事件仓储桩。 */
    final DeleteUseCaseOcrEventRepository eventRepository = new DeleteUseCaseOcrEventRepository();
}
