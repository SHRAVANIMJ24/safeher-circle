-- SafeHer Circle — directory seed data
--
-- SOURCING NOTE, and please read this before adding rows.
--
-- Every national number below was taken from the National Commission for
-- Women's own published list at ncw.gov.in/other-useful-helplines, checked in
-- August 2026. Those are marked is_verified = true.
--
-- The NGO entries are marked is_verified = FALSE. They are real organisations,
-- but their direct numbers change and this project has not phoned each one to
-- confirm. The UI shows an unverified row differently and tells the reader to
-- check before relying on it.
--
-- A wrong number here is worse than an empty table. Someone dials it, gets
-- nothing, and loses time she may not have. If you cannot source a number from
-- the organisation's own site, do not add the row.


-- ============================================================
-- NATIONAL — verified against ncw.gov.in
-- ============================================================

INSERT INTO organisations
    (name, org_type, description, phone, website, services, is_24x7, is_verified)
VALUES

    ('Emergency Response Support System',
     'HELPLINE',
     'The single national emergency number. Connects to police, fire, ambulance and other emergency services. Use this first if you are in immediate danger.',
     '112',
     'https://112.gov.in',
     ARRAY['emergency','police','ambulance'],
     TRUE, TRUE),

    ('National Women Helpline',
     'HELPLINE',
     'The national helpline for women seeking support, information or referral to local services.',
     '181',
     'https://www.ncw.gov.in',
     ARRAY['counselling','referral','support'],
     TRUE, TRUE),

    ('NCW Women Helpline',
     'HELPLINE',
     'Run by the National Commission for Women, for complaints and support relating to violence against women.',
     '7827170170',
     'https://www.ncw.gov.in/contact-us/',
     ARRAY['complaints','legal','counselling'],
     TRUE, TRUE),

    ('CHILDLINE',
     'HELPLINE',
     'For anyone under 18 in need of aid or protection, and for adults reporting a child at risk.',
     '1098',
     'https://www.childlineindia.org',
     ARRAY['child-protection','rescue'],
     TRUE, TRUE),

    ('Cyber Crime Helpline',
     'HELPLINE',
     'For online harassment, stalking, image-based abuse, impersonation and financial fraud. Complaints can also be filed online.',
     '1930',
     'https://cybercrime.gov.in',
     ARRAY['cybercrime','online-harassment'],
     TRUE, TRUE);


-- ============================================================
-- ORGANISATIONS — real, but numbers not independently confirmed.
-- Left as is_verified = FALSE until someone checks each one.
-- ============================================================

INSERT INTO organisations
    (name, org_type, description, website, city, state, services, is_24x7, is_verified)
VALUES

    ('SNEHA',
     'NGO',
     'Works on violence against women and children in Mumbai, offering counselling, crisis response and support through public hospitals.',
     'https://snehamumbai.org',
     'Mumbai', 'Maharashtra',
     ARRAY['counselling','crisis-response','health'],
     FALSE, FALSE),

    ('Majlis Legal Centre',
     'LEGAL_AID',
     'Legal support and representation for women facing violence, based in Mumbai.',
     'https://majlislaw.com',
     'Mumbai', 'Maharashtra',
     ARRAY['legal','representation'],
     FALSE, FALSE),

    ('Akshara Centre',
     'NGO',
     'Mumbai organisation working on women''s safety in public spaces, education and livelihoods.',
     'https://aksharacentre.org',
     'Mumbai', 'Maharashtra',
     ARRAY['safety','education','training'],
     FALSE, FALSE),

    ('Vanita Samaj',
     'NGO',
     'Pune-based organisation providing support and counselling for women.',
     NULL,
     'Pune', 'Maharashtra',
     ARRAY['counselling','support'],
     FALSE, FALSE),

    ('Vimochana',
     'NGO',
     'Bengaluru organisation working on violence against women, with a long-running crisis intervention service.',
     'https://vimochana.co.in',
     'Bengaluru', 'Karnataka',
     ARRAY['crisis-response','counselling','legal'],
     FALSE, FALSE),

    ('Jagori',
     'NGO',
     'Delhi-based organisation working on women''s safety, with a helpline and resources on violence and public space safety.',
     'https://jagori.org',
     'Delhi', 'Delhi',
     ARRAY['counselling','safety','resources'],
     FALSE, FALSE),

    ('Shakti Shalini',
     'SHELTER',
     'Delhi organisation providing shelter and support to survivors of violence.',
     'https://shaktishalini.org',
     'Delhi', 'Delhi',
     ARRAY['shelter','counselling','support'],
     FALSE, FALSE),

    ('Swayam',
     'NGO',
     'Kolkata organisation working to end violence against women, offering counselling and legal support.',
     'https://www.swayam.info',
     'Kolkata', 'West Bengal',
     ARRAY['counselling','legal','support'],
     FALSE, FALSE);


-- To add a verified row later, source the number from the organisation's own
-- website or a government listing, then:
--
--   UPDATE organisations
--   SET phone = '...', is_verified = TRUE
--   WHERE name = '...';
