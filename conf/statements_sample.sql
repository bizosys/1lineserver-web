use 1line;

delete from doctable where doctype = 'Q'; //Q for queries

INSERT INTO `doctable` (`id`, `parentid`,`doctype`,`title`, `document`,`status`) VALUES 
 (-21,0,'Q','company.projects','SELECT * from doctable d where d.doctype = \'P\' and d.parentid = ?','S'));


