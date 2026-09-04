DELETE rh
FROM reading_history rh
JOIN books b ON b.id = rh.book_id
JOIN content_sources s ON s.id = b.source_id
WHERE s.source_key = 'fixture-original';

DELETE rp
FROM reading_progress rp
JOIN books b ON b.id = rp.book_id
JOIN content_sources s ON s.id = b.source_id
WHERE s.source_key = 'fixture-original';

DELETE bs
FROM bookshelves bs
JOIN books b ON b.id = bs.book_id
JOIN content_sources s ON s.id = b.source_id
WHERE s.source_key = 'fixture-original';

DELETE c
FROM chapters c
JOIN books b ON b.id = c.book_id
JOIN content_sources s ON s.id = b.source_id
WHERE s.source_key = 'fixture-original';

DELETE b
FROM books b
JOIN content_sources s ON s.id = b.source_id
WHERE s.source_key = 'fixture-original';

DELETE FROM content_sources WHERE source_key = 'fixture-original';
